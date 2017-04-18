/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker.Compatibility.Incompatible
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.keysToMap

class HeaderImplDeclarationChecker(val moduleToCheck: ModuleDescriptor? = null) : DeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return

        if (descriptor !is MemberDescriptor) return

        val checkImpl = !languageVersionSettings.isFlagEnabled(AnalysisFlags.multiPlatformDoNotCheckImpl)
        if (descriptor.isHeader && declaration.hasModifier(KtTokens.HEADER_KEYWORD)) {
            checkHeaderDeclarationHasImplementation(declaration, descriptor, diagnosticHolder, checkImpl)
        }
        else if (checkImpl && descriptor.isImpl && declaration.hasModifier(KtTokens.IMPL_KEYWORD)) {
            checkImplementationHasHeaderDeclaration(declaration, descriptor, diagnosticHolder)
        }
    }

    fun checkHeaderDeclarationHasImplementation(
            reportOn: KtDeclaration, descriptor: MemberDescriptor, diagnosticHolder: DiagnosticSink, checkImpl: Boolean
    ) {
        val compatibility = findImplForHeader(descriptor, checkImpl)

        if (compatibility != null && Compatible !in compatibility) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            diagnosticHolder.report(Errors.HEADER_WITHOUT_IMPLEMENTATION.on(
                    reportOn, descriptor, moduleToCheck ?: descriptor.module, incompatibility))
        }
    }

    private fun findImplForHeader(header: MemberDescriptor, checkImpl: Boolean): Map<Compatibility, List<MemberDescriptor>>? {
        return when (header) {
            is CallableMemberDescriptor -> {
                header.findNamesakesFromTheSameModule().filter { impl ->
                    header != impl &&
                    // TODO: support non-source definitions (e.g. from Java)
                    DescriptorToSourceUtils.getSourceFromDescriptor(impl) is KtElement
                }.groupBy { impl ->
                    areCompatibleCallables(header, impl, checkImpl)
                }
            }
            is ClassDescriptor -> {
                header.findClassifiersFromTheSameModule().filter { impl ->
                    header != impl &&
                    DescriptorToSourceUtils.getSourceFromDescriptor(impl) is KtElement
                }.groupBy { impl ->
                    areCompatibleClassifiers(header, impl, checkImpl)
                }
            }
            else -> null
        }
    }

    private fun checkImplementationHasHeaderDeclaration(
            reportOn: KtDeclaration, descriptor: MemberDescriptor, diagnosticHolder: DiagnosticSink
    ) {
        val compatibility = findHeaderForImpl(descriptor)

        if (compatibility != null && Compatible !in compatibility) {
            assert(compatibility.keys.all { it is Incompatible })
            // TODO: do not report this error for members which are "almost compatible" with some header declarations
            diagnosticHolder.report(Errors.IMPLEMENTATION_WITHOUT_HEADER.on(reportOn.modifierList!!.getModifier(KtTokens.IMPL_KEYWORD)!!))
        }
    }

    private fun findHeaderForImpl(impl: MemberDescriptor): Map<Compatibility, List<MemberDescriptor>>? {
        return when (impl) {
            is CallableMemberDescriptor -> {
                val container = impl.containingDeclaration
                val candidates = when (container) {
                    is ClassDescriptor -> {
                        val headerClass = findHeaderForImpl(container)?.get(Compatible)?.firstOrNull() as? ClassDescriptor
                        headerClass?.getMembers(impl.name).orEmpty()
                    }
                    is PackageFragmentDescriptor -> impl.findNamesakesFromTheSameModule()
                    else -> return null // do not report anything for incorrect code, e.g. 'impl' local function
                }

                candidates.filter { declaration ->
                    impl != declaration && declaration.isHeader
                }.groupBy { declaration ->
                    // TODO: optimize by caching this per impl-header class pair, do not create a new substitutor for each impl member
                    val substitutor =
                            if (container is ClassDescriptor) {
                                val headerClass = declaration.containingDeclaration as ClassDescriptor
                                // TODO: this might not work for members of inner generic classes
                                Substitutor(headerClass.declaredTypeParameters, container.declaredTypeParameters)
                            }
                            else null
                    areCompatibleCallables(declaration, impl, checkImpl = false, parentSubstitutor = substitutor)
                }
            }
            is ClassifierDescriptorWithTypeParameters -> {
                impl.findClassifiersFromTheSameModule().filter { declaration ->
                    impl != declaration &&
                    declaration is ClassDescriptor && declaration.isHeader
                }.groupBy { header ->
                    areCompatibleClassifiers(header as ClassDescriptor, impl, checkImpl = false)
                }
            }
            else -> null
        }
    }

    fun MemberDescriptor.findCompatibleImplForHeader(): List<MemberDescriptor> =
            findImplForHeader(this, false)?.get(Compatible).orEmpty()

    fun MemberDescriptor.findCompatibleHeaderForImpl(): List<MemberDescriptor> =
            findHeaderForImpl(this)?.get(Compatible).orEmpty()

    private fun CallableMemberDescriptor.findNamesakesFromTheSameModule(): Collection<CallableMemberDescriptor> {
        val packageFqName = (containingDeclaration as? PackageFragmentDescriptor)?.fqName ?: return emptyList()
        val myModule = moduleToCheck ?: module
        val scope = myModule.getPackage(packageFqName).memberScope

        return when (this) {
            is FunctionDescriptor -> scope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            is PropertyDescriptor -> scope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            else -> throw AssertionError("Unsupported declaration: $this")
        } // TODO: only obtain descriptors from our module to start with
    }

    private fun ClassifierDescriptorWithTypeParameters.findClassifiersFromTheSameModule(): Collection<ClassifierDescriptorWithTypeParameters> {
        val myModule = moduleToCheck ?: module
        val classId = classId ?: return emptyList()

        fun MemberScope.getAllClassifiers(name: Name): Collection<ClassifierDescriptorWithTypeParameters> =
                getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
                        .filterIsInstance<ClassifierDescriptorWithTypeParameters>()

        val segments = classId.relativeClassName.pathSegments()
        var classifiers = myModule.getPackage(classId.packageFqName).memberScope.getAllClassifiers(segments.first())

        for (name in segments.subList(1, segments.size)) {
            classifiers = classifiers.mapNotNull { classifier ->
                (classifier as? ClassDescriptor)?.unsubstitutedInnerClassesScope?.getContributedClassifier(
                        name, NoLookupLocation.FOR_ALREADY_TRACKED
                ) as? ClassifierDescriptorWithTypeParameters
            }
        }

        return classifiers
    }

    sealed class Compatibility {
        // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
        sealed class Incompatible(
                val reason: String?,
                val unimplemented: List<Pair<CallableMemberDescriptor, Map<Incompatible, Collection<CallableMemberDescriptor>>>>? = null
        ) : Compatibility() {
            // Callables

            object ParameterShape : Incompatible("parameter shapes are different (extension vs non-extension)")

            object ParameterCount : Incompatible("number of value parameters is different")
            object TypeParameterCount : Incompatible("number of type parameters is different")

            object ParameterTypes : Incompatible("parameter types are different")
            object ReturnType : Incompatible("return type is different")

            object ParameterNames : Incompatible("parameter names are different")
            object TypeParameterNames : Incompatible("names of type parameters are different")

            object ValueParameterHasDefault : Incompatible("some parameters have default values")
            object ValueParameterModifiers : Incompatible("parameter modifiers are different (vararg, coroutine, crossinline, noinline)")

            // Functions

            object FunctionModifiersDifferent : Incompatible("modifiers are different (suspend)")
            object FunctionModifiersNotSubset : Incompatible("some modifiers on header declaration are missing on the implementation (external, infix, inline, operator, tailrec)")

            // Properties

            object PropertyKind : Incompatible("property kinds are different (val vs var)")
            object PropertyModifiers : Incompatible("modifiers are different (const, lateinit)")

            // Classifiers

            object ClassKind : Incompatible("class kinds are different (class, interface, object, enum, annotation)")

            object ClassModifiers : Incompatible("modifiers are different (companion, inner)")

            object Supertypes : Incompatible("some supertypes are missing in the implementation")

            class ClassScopes(
                    unimplemented: List<Pair<CallableMemberDescriptor, Map<Incompatible, Collection<CallableMemberDescriptor>>>>
            ) : Incompatible("some members are not implemented", unimplemented)

            object EnumEntries : Incompatible("some entries from header enum are missing in the impl enum")

            // Common

            object Modality : Incompatible("modality is different")
            object Visibility : Incompatible("visibility is different")

            object TypeParameterUpperBounds : Incompatible("upper bounds of type parameters are different")
            object TypeParameterVariance : Incompatible("declaration-site variances of type parameters are different")
            object TypeParameterReified : Incompatible("some type parameter is reified in one declaration and non-reified in the other")

            object NoImpl : Incompatible("the implementation is not marked with the 'impl' modifier (-Xno-check-impl)")

            object Unknown : Incompatible(null)
        }

        object Compatible : Compatibility()
    }

    // a is the declaration in common code, b is the definition in the platform-specific code
    private fun areCompatibleCallables(
            a: CallableMemberDescriptor,
            b: CallableMemberDescriptor,
            checkImpl: Boolean,
            parentSubstitutor: Substitutor? = null
    ): Compatibility {
        assert(a.name == b.name) { "This function should be invoked only for declarations with the same name: $a, $b" }
        assert(a.containingDeclaration is ClassDescriptor == b.containingDeclaration is ClassDescriptor) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $a, $b"
        }

        val aExtensionReceiver = a.extensionReceiverParameter
        val bExtensionReceiver = b.extensionReceiverParameter
        if ((aExtensionReceiver != null) != (bExtensionReceiver != null)) return Incompatible.ParameterShape

        val aParams = a.valueParameters
        val bParams = b.valueParameters
        if (aParams.size != bParams.size) return Incompatible.ParameterCount

        val aTypeParams = a.typeParameters
        val bTypeParams = b.typeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        val substitutor = Substitutor(aTypeParams, bTypeParams, parentSubstitutor)

        if (!areCompatibleTypeLists(aParams.map { substitutor(it.type) }, bParams.map { it.type }) ||
            !areCompatibleTypes(aExtensionReceiver?.type?.let(substitutor), bExtensionReceiver?.type)) return Incompatible.ParameterTypes
        if (!areCompatibleTypes(substitutor(a.returnType), b.returnType)) return Incompatible.ReturnType

        if (b.hasStableParameterNames() && !equalsBy(aParams, bParams, ValueParameterDescriptor::getName)) return Incompatible.ParameterNames
        if (!equalsBy(aTypeParams, bTypeParams, TypeParameterDescriptor::getName)) return Incompatible.TypeParameterNames

        if (a.modality != b.modality) return Incompatible.Modality
        if (a.visibility != b.visibility) return Incompatible.Visibility

        areCompatibleTypeParameters(aTypeParams, bTypeParams, substitutor).let { if (it != Compatible) return it }

        if (!equalsBy(aParams, bParams, ValueParameterDescriptor::declaresDefaultValue)) return Incompatible.ValueParameterHasDefault
        if (!equalsBy(aParams, bParams, { p -> listOf(p.varargElementType != null, p.isCrossinline, p.isNoinline) })) return Incompatible.ValueParameterModifiers

        when {
            a is FunctionDescriptor && b is FunctionDescriptor -> areCompatibleFunctions(a, b).let { if (it != Compatible) return it }
            a is PropertyDescriptor && b is PropertyDescriptor -> areCompatibleProperties(a, b).let { if (it != Compatible) return it }
            else -> throw AssertionError("Unsupported declarations: $a, $b")
        }

        if (checkImpl && !b.isImpl && b.kind == CallableMemberDescriptor.Kind.DECLARATION) return Incompatible.NoImpl

        return Compatible
    }

    private fun areCompatibleTypes(a: KotlinType?, b: KotlinType?): Boolean {
        return if (a != null) b != null && TypeUtils.equalTypes(a, b) else b == null
    }

    private fun areCompatibleTypeLists(a: List<KotlinType?>, b: List<KotlinType?>): Boolean {
        for (i in a.indices) {
            if (!areCompatibleTypes(a[i], b[i])) return false
        }
        return true
    }

    private fun areCompatibleTypeParameters(a: List<TypeParameterDescriptor>, b: List<TypeParameterDescriptor>, substitutor: Substitutor): Compatibility {
        if (!areCompatibleTypeLists(a.map { substitutor(it.defaultType) }, b.map { it.defaultType })) return Incompatible.TypeParameterUpperBounds
        if (!equalsBy(a, b, TypeParameterDescriptor::getVariance)) return Incompatible.TypeParameterVariance
        if (!equalsBy(a, b, TypeParameterDescriptor::isReified)) return Incompatible.TypeParameterReified

        return Compatible
    }

    private fun areCompatibleFunctions(a: FunctionDescriptor, b: FunctionDescriptor): Compatibility {
        if (!equalBy(a, b) { f -> f.isSuspend }) return Incompatible.FunctionModifiersDifferent

        if (a.isExternal && !b.isExternal ||
            a.isInfix && !b.isInfix ||
            a.isInline && !b.isInline ||
            a.isOperator && !b.isOperator ||
            a.isTailrec && !b.isTailrec) return Incompatible.FunctionModifiersNotSubset

        return Compatible
    }

    private fun areCompatibleProperties(a: PropertyDescriptor, b: PropertyDescriptor): Compatibility {
        if (!equalBy(a, b) { p -> p.isVar }) return Incompatible.PropertyKind
        if (!equalBy(a, b) { p -> listOf(p.isConst, p.isLateInit) }) return Incompatible.PropertyModifiers

        return Compatible
    }

    private fun areCompatibleClassifiers(a: ClassDescriptor, other: ClassifierDescriptor, checkImpl: Boolean): Compatibility {
        assert(a.fqNameUnsafe == other.fqNameUnsafe) { "This function should be invoked only for declarations with the same name: $a, $other" }

        var parentSubstitutor: Substitutor? = null
        var implTypealias = false

        val b = when (other) {
            is ClassDescriptor -> other
            is TypeAliasDescriptor -> {
                val classDescriptor = other.classDescriptor ?: return Compatible // do not report extra error on erroneous typealias
                implTypealias = true
                // If a platform class test.C is implemented by a typealias test.C = test.CImpl, we must now state that any occurrence
                // of the type "test.C" in the platform class scope should be replaced with "test.CImpl".
                // Otherwise the types would not be equal and e.g. test.C's constructor is not going to be found in test.CImpl's scope.
                // For this, we construct an additional substitutor with a single mapping test.C -> test.CImpl
                // TODO: this looks like a dirty hack
                parentSubstitutor = Substitutor(null, TypeSubstitutor.create(TypeConstructorSubstitution.createByConstructorsMap(
                        mapOf(a.typeConstructor to classDescriptor.defaultType.asTypeProjection())
                )))
                classDescriptor
            }
            else -> throw AssertionError("Incorrect impl classifier for $a: $other")
        }

        if (a.kind != b.kind) return Incompatible.ClassKind

        if (!equalBy(a, b) { listOf(it.isCompanionObject, it.isInner) }) return Incompatible.ClassModifiers

        val aTypeParams = a.declaredTypeParameters
        val bTypeParams = b.declaredTypeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        if (a.modality != b.modality && !(a.modality == Modality.FINAL && b.modality == Modality.OPEN)) return Incompatible.Modality

        if (a.visibility != b.visibility) return Incompatible.Visibility

        val substitutor = Substitutor(aTypeParams, bTypeParams, parentSubstitutor)
        areCompatibleTypeParameters(aTypeParams, bTypeParams, substitutor).let { if (it != Compatible) return it }

        // Subtract kotlin.Any from supertypes because it's implicitly added if no explicit supertype is specified,
        // and not added if an explicit supertype _is_ specified
        val aSupertypes = a.typeConstructor.supertypes.filterNot(KotlinBuiltIns::isAny)
        val bSupertypes = b.typeConstructor.supertypes.filterNot(KotlinBuiltIns::isAny)
        if (aSupertypes.map(substitutor).any { aSupertype ->
            bSupertypes.none { bSupertype -> areCompatibleTypes(aSupertype, bSupertype) }
        }) return Incompatible.Supertypes

        areCompatibleClassScopes(a, b, checkImpl && !implTypealias, substitutor).let { if (it != Compatible) return it }

        if (checkImpl && !b.isImpl && !implTypealias) return Incompatible.NoImpl

        return Compatible
    }

    private fun areCompatibleClassScopes(
            a: ClassDescriptor,
            b: ClassDescriptor,
            checkImpl: Boolean,
            substitutor: Substitutor
    ): Compatibility {
        val unimplemented = arrayListOf<Pair<CallableMemberDescriptor, Map<Incompatible, MutableCollection<CallableMemberDescriptor>>>>()

        val bMembersByName = b.getMembers().groupBy { it.name }

        outer@ for (aMember in a.getMembers()) {
            if (!aMember.kind.isReal) continue

            val mapping = bMembersByName[aMember.name].orEmpty().keysToMap { bMember ->
                areCompatibleCallables(aMember, bMember, checkImpl, substitutor)
            }
            if (mapping.values.any { it == Compatible }) continue

            val incompatibilityMap = mutableMapOf<Incompatible, MutableCollection<CallableMemberDescriptor>>()
            for ((descriptor, compatibility) in mapping) {
                when (compatibility) {
                    Compatible -> continue@outer
                    is Incompatible -> incompatibilityMap.getOrPut(compatibility) { SmartList() }.add(descriptor)
                }
            }

            unimplemented.add(aMember to incompatibilityMap)
        }

        if (a.kind == ClassKind.ENUM_CLASS) {
            fun ClassDescriptor.enumEntries() =
                    unsubstitutedMemberScope.getDescriptorsFiltered().filter(DescriptorUtils::isEnumEntry).map { it.name }
            val aEntries = a.enumEntries()
            val bEntries = b.enumEntries()

            if (!bEntries.containsAll(aEntries)) return Incompatible.EnumEntries
        }

        // TODO: check static scope?

        if (unimplemented.isEmpty()) return Compatible

        return Incompatible.ClassScopes(unimplemented)
    }

    private fun ClassDescriptor.getMembers(name: Name? = null): Collection<CallableMemberDescriptor> {
        val nameFilter = if (name != null) { it -> it == name } else MemberScope.ALL_NAME_FILTER
        return defaultType.memberScope.getDescriptorsFiltered(nameFilter = nameFilter).filterIsInstance<CallableMemberDescriptor>() +
               constructors.filter { nameFilter(it.name) }
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
            selector(first) == selector(second)

    private inline fun <T, K> equalsBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return false
        }

        return true
    }

    // This substitutor takes the type from A's signature and returns the type that should be in that place in B's signature
    private class Substitutor(
            private val parent: Substitutor?,
            private val typeSubstitutor: TypeSubstitutor
    ) : (KotlinType?) -> KotlinType? {
        constructor(
                aTypeParams: List<TypeParameterDescriptor>,
                bTypeParams: List<TypeParameterDescriptor>,
                parent: Substitutor? = null
        ) : this(parent, TypeSubstitutor.create(
                TypeConstructorSubstitution.createByParametersMap(aTypeParams.keysToMap {
                    bTypeParams[it.index].defaultType.asTypeProjection()
                })
        ))

        override fun invoke(type: KotlinType?): KotlinType? =
                (parent?.invoke(type) ?: type)?.asTypeProjection()?.let(typeSubstitutor::substitute)?.type
    }
}
