/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibilityChecker.Substitutor
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Compatible

/**
 * Convenience shortcuts
 *
 * Essentially they just delegate to [ExpectedActualResolver], and perform some trivial
 * utility-work (like resolving only compatible expects/actuals, etc.)
 */

// FIXME(dsavvinov): review clients, as they won't work properly in HMPP projects
fun MemberDescriptor.findCompatibleActualForExpected(platformModule: ModuleDescriptor): List<MemberDescriptor> =
    ExpectedActualResolver.findActualForExpected(this, platformModule)?.get(Compatible).orEmpty()

fun MemberDescriptor.findCompatibleExpectedForActual(commonModule: ModuleDescriptor): List<MemberDescriptor> =
    ExpectedActualResolver.findExpectedForActual(this, commonModule)?.get(Compatible).orEmpty()

fun MemberDescriptor.findAnyActualForExpected(platformModule: ModuleDescriptor): List<MemberDescriptor> {
    val actualsGroupedByCompatibility = ExpectedActualResolver.findActualForExpected(this, platformModule)
    return actualsGroupedByCompatibility?.get(Compatible)
        ?: actualsGroupedByCompatibility?.values?.flatten()
        ?: emptyList()
}

fun DeclarationDescriptor.findExpects(inModule: ModuleDescriptor = this.module): List<MemberDescriptor> {
    return ExpectedActualResolver.findExpectedForActual(
        this as MemberDescriptor,
        inModule
    )?.get(Compatible).orEmpty()
}

fun DeclarationDescriptor.findActuals(inModule: ModuleDescriptor = this.module): List<MemberDescriptor> {
    return ExpectedActualResolver.findActualForExpected(
        (this as MemberDescriptor),
        inModule
    )?.get(Compatible).orEmpty()
}


/**
 * Facade for getting information for expect-actual matching.
 *
 * It's work mostly consists of two large parts:
 * - find potentially compatible declarations by querying scopes
 * - compute compatibility of those declarations by querying[ExpectActualCompatibilityChecker]
 */
object ExpectedActualResolver {
    fun findActualForExpected(
        expected: MemberDescriptor,
        platformModule: ModuleDescriptor
    ): Map<Compatibility, List<MemberDescriptor>>? {
        val compatibilityChecker = ExpectActualCompatibilityChecker(platformModule)
        return when (expected) {
            is CallableMemberDescriptor -> {
                expected.findNamesakesFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                            // TODO: use some other way to determine that the declaration is from Kotlin.
                            //       This way behavior differs between fast and PSI-based Java class reading mode
                            // TODO: support non-source definitions (e.g. from Java)
                            actual.couldHaveASource
                }.groupBy { actual ->
                    compatibilityChecker.areCompatibleCallables(expected, actual)
                }
            }
            is ClassDescriptor -> {
                expected.findClassifiersFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                            actual.couldHaveASource
                }.groupBy { actual ->
                    compatibilityChecker.areCompatibleClassifiers(expected, actual)
                }
            }
            else -> null
        }
    }

    fun findExpectedForActual(
        actual: MemberDescriptor,
        commonModule: ModuleDescriptor
    ): Map<Compatibility, List<MemberDescriptor>>? {
        val compatibilityChecker = ExpectActualCompatibilityChecker(actual.module)

        return when (actual) {
            is CallableMemberDescriptor -> {
                val container = actual.containingDeclaration
                val candidates = when (container) {
                    is ClassifierDescriptorWithTypeParameters -> {
                        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
                        val expectedClass =
                            findExpectedForActual(container, commonModule)?.values?.firstOrNull()?.firstOrNull() as? ClassDescriptor
                        expectedClass?.getMembers(actual.name)?.filterIsInstance<CallableMemberDescriptor>().orEmpty()
                    }
                    is PackageFragmentDescriptor -> actual.findNamesakesFromModule(commonModule)
                    else -> return null // do not report anything for incorrect code, e.g. 'actual' local function
                }

                candidates.filter { declaration ->
                    actual != declaration && declaration.isExpect
                }.groupBy { declaration ->
                    // TODO: optimize by caching this per actual-expected class pair, do not create a new substitutor for each actual member
                    val substitutor =
                        if (container is ClassDescriptor) {
                            val expectedClass = declaration.containingDeclaration as ClassDescriptor
                            // TODO: this might not work for members of inner generic classes
                            Substitutor(expectedClass.declaredTypeParameters, container.declaredTypeParameters)
                        } else null
                    compatibilityChecker.areCompatibleCallables(declaration, actual, parentSubstitutor = substitutor)
                }
            }
            is ClassifierDescriptorWithTypeParameters -> {
                actual.findClassifiersFromModule(commonModule).filter { declaration ->
                    actual != declaration &&
                            declaration is ClassDescriptor && declaration.isExpect
                }.groupBy { expected ->
                    compatibilityChecker.areCompatibleClassifiers(expected as ClassDescriptor, actual)
                }
            }
            else -> null
        }
    }

    sealed class Compatibility {
        // For IncompatibilityKind.STRONG `actual` declaration is considered as overload and error reports on expected declaration
        enum class IncompatibilityKind {
            WEAK, STRONG
        }

        // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
        sealed class Incompatible(val reason: String?, val kind: IncompatibilityKind = IncompatibilityKind.WEAK) : Compatibility() {
            // Callables

            object CallableKind : Incompatible("callable kinds are different (function vs property)", IncompatibilityKind.STRONG)

            object ParameterShape :
                Incompatible("parameter shapes are different (extension vs non-extension)", IncompatibilityKind.STRONG)

            object ParameterCount : Incompatible("number of value parameters is different", IncompatibilityKind.STRONG)
            object TypeParameterCount : Incompatible("number of type parameters is different", IncompatibilityKind.STRONG)

            object ParameterTypes : Incompatible("parameter types are different", IncompatibilityKind.STRONG)
            object ReturnType : Incompatible("return type is different", IncompatibilityKind.STRONG)

            object ParameterNames : Incompatible("parameter names are different")
            object TypeParameterNames : Incompatible("names of type parameters are different")

            object ValueParameterVararg : Incompatible("some value parameter is vararg in one declaration and non-vararg in the other")
            object ValueParameterNoinline :
                Incompatible("some value parameter is noinline in one declaration and not noinline in the other")

            object ValueParameterCrossinline :
                Incompatible("some value parameter is crossinline in one declaration and not crossinline in the other")

            // Functions

            object FunctionModifiersDifferent : Incompatible("modifiers are different (suspend)")
            object FunctionModifiersNotSubset :
                Incompatible("some modifiers on expected declaration are missing on the actual one (external, infix, inline, operator, tailrec)")

            // Properties

            object PropertyKind : Incompatible("property kinds are different (val vs var)")
            object PropertyModifiers : Incompatible("modifiers are different (const, lateinit)")

            // Classifiers

            object ClassKind : Incompatible("class kinds are different (class, interface, object, enum, annotation)")

            object ClassModifiers : Incompatible("modifiers are different (companion, inner, inline)")

            object Supertypes : Incompatible("some supertypes are missing in the actual declaration")

            class ClassScopes(
                val unfulfilled: List<Pair<MemberDescriptor, Map<Incompatible, Collection<MemberDescriptor>>>>
            ) : Incompatible("some expected members have no actual ones")

            object EnumEntries : Incompatible("some entries from expected enum are missing in the actual enum")

            // Common

            object Modality : Incompatible("modality is different")
            object Visibility : Incompatible("visibility is different")

            object TypeParameterUpperBounds : Incompatible("upper bounds of type parameters are different", IncompatibilityKind.STRONG)
            object TypeParameterVariance : Incompatible("declaration-site variances of type parameters are different")
            object TypeParameterReified : Incompatible("some type parameter is reified in one declaration and non-reified in the other")

            object Unknown : Incompatible(null)
        }

        object Compatible : Compatibility()
    }
}
