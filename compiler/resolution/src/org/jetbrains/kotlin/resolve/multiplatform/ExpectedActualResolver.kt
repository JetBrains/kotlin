/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Compatible
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object ExpectedActualResolver {
    fun findActualForExpected(
        expected: MemberDescriptor,
        platformModule: ModuleDescriptor,
        moduleVisibilityFilter: ModuleFilter = allModulesProvidingActualsFor(expected.module, platformModule),
    ): Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>? {
        val context = ClassicExpectActualMatchingContext(platformModule)
        return when (expected) {
            is CallableMemberDescriptor -> {
                expected.findNamesakesFromModule(context, platformModule, moduleVisibilityFilter).filter { actual ->
                    expected != actual && !actual.isExpect &&
                            // TODO: use some other way to determine that the declaration is from Kotlin.
                            //       This way behavior differs between fast and PSI-based Java class reading mode
                            // TODO: support non-source definitions (e.g. from Java)
                            actual.couldHaveASource
                }.groupBy { actual ->
                    AbstractExpectActualCompatibilityChecker.areCompatibleCallables(
                        expected,
                        actual,
                        parentSubstitutor = null,
                        expectContainingClass = null,
                        actualContainingClass = null,
                        context
                    )
                }
            }
            is ClassDescriptor -> {
                context.findClassifiersFromModule(expected.classId, platformModule, moduleVisibilityFilter).filter { actual ->
                    expected != actual && !actual.isExpect && actual.couldHaveASource
                }.groupBy { actual ->
                    AbstractExpectActualCompatibilityChecker.areCompatibleClassifiers(
                        expected,
                        actual,
                        context
                    )
                }
            }
            else -> null
        }
    }

    fun findExpectedForActual(
        actual: MemberDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean = allModulesProvidingExpectsFor(actual.module)
    ): Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>? {
        val context = ClassicExpectActualMatchingContext(actual.module)
        return when (actual) {
            is CallableMemberDescriptor -> {
                val container = actual.containingDeclaration
                val candidates = when (container) {
                    is ClassifierDescriptorWithTypeParameters -> {
                        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
                        val expectedClass =
                            findExpectedForActual(container, moduleFilter)?.values?.firstOrNull()?.firstOrNull() as? ClassDescriptor
                        with(context) {
                            expectedClass?.getMembersForExpectClass(actual.name)?.filterIsInstance<CallableMemberDescriptor>().orEmpty()
                        }
                    }
                    is PackageFragmentDescriptor -> actual.findNamesakesFromModule(context, actual.module, moduleFilter)
                    else -> return null // do not report anything for incorrect code, e.g. 'actual' local function
                }

                candidates.filter { declaration ->
                    actual != declaration && declaration.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE && declaration.isExpect
                }.groupBy { declaration ->
                    // TODO: optimize by caching this per actual-expected class pair, do not create a new substitutor for each actual member
                    var expectedClass: ClassDescriptor? = null
                    var actualClass: ClassDescriptor? = null
                    val substitutor =
                        when (container) {
                            is ClassDescriptor -> {
                                actualClass = container
                                expectedClass = declaration.containingDeclaration as ClassDescriptor
                                // TODO: this might not work for members of inner generic classes
                                runIf(expectedClass.declaredTypeParameters.size == container.declaredTypeParameters.size) {
                                    context.createExpectActualTypeParameterSubstitutor(
                                        expectedClass.declaredTypeParameters,
                                        container.declaredTypeParameters,
                                        parentSubstitutor = null
                                    )
                                }
                            }
                            else -> null
                        }
                    AbstractExpectActualCompatibilityChecker.areCompatibleCallables(
                        expectDeclaration = declaration,
                        actualDeclaration = actual,
                        parentSubstitutor = substitutor,
                        expectContainingClass = expectedClass,
                        actualContainingClass = actualClass,
                        context
                    )
                }
            }
            is ClassifierDescriptorWithTypeParameters -> {
                context.findClassifiersFromModule(actual.classId, actual.module, moduleFilter).filter { declaration ->
                    actual != declaration && declaration is ClassDescriptor && declaration.isExpect
                }.groupBy { expected ->
                    AbstractExpectActualCompatibilityChecker.areCompatibleClassifiers(
                        expected as ClassDescriptor,
                        actual,
                        context
                    )
                }
            }
            else -> null
        }
    }

    private fun CallableMemberDescriptor.findNamesakesFromModule(
        context: ClassicExpectActualMatchingContext,
        module: ModuleDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean
    ): Collection<CallableMemberDescriptor> {
        val scopes = when (val containingDeclaration = containingDeclaration) {
            is PackageFragmentDescriptor -> {
                listOf(module.getPackage(containingDeclaration.fqName).memberScope)
            }
            is ClassDescriptor -> {
                val classes = context.findClassifiersFromModule(containingDeclaration.classId, module, moduleFilter)
                    .mapNotNull { if (it is TypeAliasDescriptor) it.classDescriptor else it }
                    .filterIsInstance<ClassDescriptor>()
                if (this is ConstructorDescriptor) return classes.flatMap { it.constructors }

                classes.map { it.unsubstitutedMemberScope }
            }
            else -> return emptyList()
        }

        return when (this) {
            is FunctionDescriptor -> scopes.flatMap { scope ->
                scope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { it == name }
                    .filter { it.name == name }
                    .filterIsInstance<CallableMemberDescriptor>()
            }

            is PropertyDescriptor -> scopes.flatMap { scope ->
                scope.getContributedDescriptors(DescriptorKindFilter.VARIABLES) { it == name }
                    .filter { it.name == name }
                    .filterIsInstance<CallableMemberDescriptor>()
            }

            else -> throw AssertionError("Unsupported declaration: $this")
        }.applyFilter(moduleFilter)
    }
}

// FIXME(dsavvinov): review clients, as they won't work properly in HMPP projects
@JvmOverloads
fun MemberDescriptor.findCompatibleActualsForExpected(
    platformModule: ModuleDescriptor, moduleFilter: ModuleFilter = allModulesProvidingActualsFor(module, platformModule)
): List<MemberDescriptor> =
    ExpectedActualResolver.findActualForExpected(this, platformModule, moduleFilter)?.get(Compatible).orEmpty()

@JvmOverloads
fun MemberDescriptor.findAnyActualsForExpected(
    platformModule: ModuleDescriptor, moduleFilter: ModuleFilter = allModulesProvidingActualsFor(module, platformModule)
): List<MemberDescriptor> {
    val actualsGroupedByCompatibility = ExpectedActualResolver.findActualForExpected(this, platformModule, moduleFilter)
    return actualsGroupedByCompatibility?.get(Compatible)
        ?: actualsGroupedByCompatibility?.values?.flatten()
        ?: emptyList()
}

fun MemberDescriptor.findCompatibleExpectsForActual(
    moduleFilter: ModuleFilter = allModulesProvidingExpectsFor(module)
): List<MemberDescriptor> =
    ExpectedActualResolver.findExpectedForActual(this, moduleFilter)?.get(Compatible).orEmpty()

fun DeclarationDescriptor.findExpects(): List<MemberDescriptor> {
    if (this !is MemberDescriptor) return emptyList()
    return this.findCompatibleExpectsForActual()
}

fun DeclarationDescriptor.findActuals(inModule: ModuleDescriptor): List<MemberDescriptor> {
    if (this !is MemberDescriptor) return emptyList()
    return this.findCompatibleActualsForExpected(inModule)
}

// TODO: Klibs still need to better handle source in deserialized descriptors.
val DeclarationDescriptorWithSource.couldHaveASource: Boolean
    get() = this.source.containingFile != SourceFile.NO_SOURCE_FILE ||
            this is DeserializedDescriptor
