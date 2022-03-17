/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.components.KtBuiltinTypes
import org.jetbrains.kotlin.analysis.api.components.KtTypeProvider
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescSyntheticFieldSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.isInterfaceLike
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.PublicApproximatorConfiguration
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.util.containingNonLocalDeclaration

internal class KtFe10TypeProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtTypeProvider(), Fe10KtAnalysisSessionComponent {
    @Suppress("SpellCheckingInspection")
    private val typeApproximator by lazy {
        TypeApproximator(
            analysisContext.builtIns,
            analysisContext.resolveSession.languageVersionSettings
        )
    }

    override val token: ValidityToken
        get() = analysisSession.token

    override val builtinTypes: KtBuiltinTypes by cached { KtFe10BuiltinTypes(analysisContext) }

    override fun approximateToSuperPublicDenotableType(type: KtType): KtType? = withValidityAssertion {
        require(type is KtFe10Type)
        return typeApproximator.approximateToSuperType(type.type, PublicApproximatorConfiguration)?.toKtType(analysisContext)
    }

    override fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType = withValidityAssertion {
        val kotlinType = (getSymbolDescriptor(symbol) as? ClassDescriptor)?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, symbol.nameOrAnonymous.toString())

        return kotlinType.toKtType(analysisContext)
    }

    override fun commonSuperType(types: Collection<KtType>): KtType = withValidityAssertion {
        val kotlinTypes = types.map { (it as KtFe10Type).type }
        return CommonSupertypes.commonSupertype(kotlinTypes).toKtType(analysisContext)
    }

    override fun getKtType(ktTypeReference: KtTypeReference): KtType = withValidityAssertion {
        val bindingContext = analysisContext.analyze(ktTypeReference, AnalysisMode.PARTIAL)
        val kotlinType = bindingContext[BindingContext.TYPE, ktTypeReference]
            ?: ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, ktTypeReference.text)
        return kotlinType.toKtType(analysisContext)
    }

    override fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KtType? = withValidityAssertion {
        val bindingContext = analysisContext.analyze(expression, AnalysisMode.PARTIAL)
        val lhs = bindingContext[BindingContext.DOUBLE_COLON_LHS, expression] ?: return null
        return lhs.type.toKtType(analysisContext)
    }

    override fun withNullability(type: KtType, newNullability: KtTypeNullability): KtType = withValidityAssertion {
        require(type is KtFe10Type)
        return type.type.makeNullableAsSpecified(newNullability == KtTypeNullability.NULLABLE).toKtType(analysisContext)
    }

    override fun haveCommonSubtype(a: KtType, b: KtType): Boolean {
        return areTypesCompatible((a as KtFe10Type).type, (b as KtFe10Type).type)
    }

    override fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KtType> {
        val elementToAnalyze = position.containingNonLocalDeclaration() ?: position
        val bindingContext = analysisContext.analyze(elementToAnalyze)

        val lexicalScope = position.getResolutionScope(bindingContext) ?: return emptyList()
        return lexicalScope.getImplicitReceiversHierarchy().map { it.type.toKtType(analysisContext) }
    }

    override fun getDirectSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType> {
        require(type is KtFe10Type)
        return TypeUtils.getImmediateSupertypes(type.type).map { it.toKtType(analysisContext) }
    }

    override fun getAllSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType> {
        require(type is KtFe10Type)
        return TypeUtils.getAllSupertypes(type.type).map { it.toKtType(analysisContext) }
    }

    override fun getDispatchReceiverType(symbol: KtCallableSymbol): KtType? {
        assertIsValidAndAccessible()
        require(symbol is KtFe10Symbol)

        val descriptor = when (symbol) {
            is KtFe10DescSymbol<*> -> symbol.descriptor as? CallableDescriptor
            is KtFe10PsiSymbol<*, *> -> symbol.descriptor as? CallableDescriptor
            is KtFe10DescSyntheticFieldSymbol -> symbol.descriptor
            else -> error("No callable descriptor on $symbol")
        }

        return descriptor?.dispatchReceiverParameter?.type?.toKtType(analysisContext)
    }

    private fun areTypesCompatible(a: KotlinType, b: KotlinType): Boolean {
        if (a.isNothing() || b.isNothing() || TypeUtils.equalTypes(a, b) || (a.isNullable() && b.isNullable())) {
            return true
        }

        val aConstructor = a.constructor
        val bConstructor = b.constructor

        if (aConstructor is IntersectionTypeConstructor) {
            return aConstructor.supertypes.all { areTypesCompatible(it, b) }
        }

        if (bConstructor is IntersectionTypeConstructor) {
            return bConstructor.supertypes.all { areTypesCompatible(a, it) }
        }

        val intersectionType = TypeIntersector.intersectTypes(listOf(a, b)) ?: return false
        val intersectionTypeConstructor = intersectionType.constructor

        if (intersectionTypeConstructor is IntersectionTypeConstructor) {
            val intersectedTypes = intersectionTypeConstructor.supertypes
            if (intersectedTypes.all { it.isNullable() }) {
                return true
            }

            val collectedUpperBounds = intersectedTypes.flatMapTo(mutableSetOf()) { getUpperBounds(it) }
            return areBoundsCompatible(collectedUpperBounds, emptySet())
        } else {
            return !intersectionType.isNothing()
        }
    }

    private fun getUpperBounds(type: KotlinType): List<KotlinType> {
        when (type) {
            is FlexibleType -> return getUpperBounds(type.upperBound)
            is DefinitelyNotNullType -> return getUpperBounds(type.original)
            is ErrorType -> return emptyList()
            is CapturedType -> return type.constructor.supertypes.flatMap { getUpperBounds(it) }
            is NewCapturedType -> return type.constructor.supertypes.flatMap { getUpperBounds(it) }
            is SimpleType -> {
                val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
                if (typeParameterDescriptor != null) {
                    return typeParameterDescriptor.upperBounds.flatMap { getUpperBounds(it) }
                }

                val typeConstructor = type.constructor
                if (typeConstructor is NewTypeVariableConstructor) {
                    return typeConstructor.originalTypeParameter?.upperBounds.orEmpty().flatMap { getUpperBounds(it) }
                }
                if (typeConstructor is IntersectionTypeConstructor) {
                    return typeConstructor.supertypes.flatMap { getUpperBounds(it) }
                }

                return listOf(type)
            }
            else -> return emptyList()
        }
    }

    private fun areBoundsCompatible(
        upperBounds: Set<KotlinType>,
        lowerBounds: Set<KotlinType>,
        checkedTypeParameters: MutableSet<TypeParameterDescriptor> = mutableSetOf()
    ): Boolean {
        val upperBoundClasses = upperBounds.mapNotNull { getBoundClass(it) }.toSet()

        val leafClassesOrInterfaces = computeLeafClassesOrInterfaces(upperBoundClasses)
        if (areClassesOrInterfacesIncompatible(leafClassesOrInterfaces)) {
            return false
        }

        if (!lowerBounds.all { lowerBoundType ->
                val classesSatisfyingLowerBounds = collectSuperClasses(lowerBoundType)
                leafClassesOrInterfaces.all { it in classesSatisfyingLowerBounds }
            }
        ) {
            return false
        }

        if (upperBounds.size < 2) {
            return true
        }

        val typeArgumentMapping = collectTypeArgumentMapping(upperBounds)
        for ((typeParameter, boundTypeArguments) in typeArgumentMapping) {
            if (!boundTypeArguments.isCompatible) {
                return false
            }

            checkedTypeParameters.add(typeParameter)
            if (!areBoundsCompatible(boundTypeArguments.upper, boundTypeArguments.lower, checkedTypeParameters)) {
                return false
            }
        }

        return true
    }

    private fun collectTypeArgumentMapping(upperBounds: Set<KotlinType>): Map<TypeParameterDescriptor, BoundTypeArguments> {
        val typeArgumentMapping = LinkedHashMap<TypeParameterDescriptor, BoundTypeArguments>()
        for (type in upperBounds) {
            val mappingForType = type.toTypeArgumentMapping() ?: continue

            val queue = ArrayDeque<TypeArgumentMapping>()
            queue.addLast(mappingForType)

            while (queue.isNotEmpty()) {
                val (typeParameterOwner, mapping) = queue.removeFirst()
                for (superType in typeParameterOwner.typeConstructor.supertypes) {
                    val mappingForSupertype = superType.toTypeArgumentMapping(mapping) ?: continue
                    queue.addLast(mappingForSupertype)
                }

                for ((typeParameterDescriptor, boundTypeArgument) in mapping) {
                    val boundsForParameter = typeArgumentMapping.computeIfAbsent(typeParameterDescriptor) {
                        var isCompatible = true
                        val languageVersionSettings = analysisContext.resolveSession.languageVersionSettings
                        if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleEnums)) {
                            isCompatible = isCompatible && typeParameterOwner.classId != StandardClassIds.Enum
                        }
                        if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitComparisonOfIncompatibleClasses)) {
                            isCompatible = isCompatible && typeParameterOwner.classId != StandardClassIds.KClass
                        }

                        BoundTypeArguments(mutableSetOf(), mutableSetOf(), isCompatible)
                    }

                    if (boundTypeArgument.variance.allowsOutPosition) {
                        boundsForParameter.upper += boundTypeArgument.type.collectUpperBounds()
                    }

                    if (boundTypeArgument.variance.allowsInPosition) {
                        boundsForParameter.lower += boundTypeArgument.type.collectLowerBounds()
                    }
                }
            }

        }
        return typeArgumentMapping
    }

    private fun KotlinType.collectLowerBounds(): Set<KotlinType> {
        when (this) {
            is FlexibleType -> return lowerBound.collectLowerBounds()
            is DefinitelyNotNullType -> return original.collectLowerBounds()
            is ErrorType -> return emptySet()
            is CapturedType, is NewCapturedType -> return constructor.supertypes.flatMapTo(mutableSetOf()) { it.collectLowerBounds() }
            is SimpleType -> {
                val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(this)
                if (typeParameterDescriptor != null) {
                    return emptySet()
                }

                return when (val typeConstructor = this.constructor) {
                    is NewTypeVariableConstructor -> emptySet()
                    is IntersectionTypeConstructor -> typeConstructor.supertypes.flatMapTo(mutableSetOf()) { it.collectLowerBounds() }
                    else -> setOf(this)
                }

            }
            else -> return emptySet()
        }
    }

    private fun KotlinType.collectUpperBounds(): Set<KotlinType> {
        when (this) {
            is FlexibleType -> return lowerBound.collectUpperBounds()
            is DefinitelyNotNullType -> return original.collectUpperBounds()
            is ErrorType -> return emptySet()
            is CapturedType, is NewCapturedType -> return constructor.supertypes.flatMapTo(mutableSetOf()) { it.collectUpperBounds() }
            is SimpleType -> {
                val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(this)
                if (typeParameterDescriptor != null) {
                    return typeParameterDescriptor.upperBounds.flatMapTo(mutableSetOf()) { it.collectUpperBounds() }
                }

                return when (val typeConstructor = this.constructor) {
                    is NewTypeVariableConstructor -> typeConstructor.supertypes.flatMapTo(mutableSetOf()) { it.collectUpperBounds() }
                    is IntersectionTypeConstructor -> typeConstructor.supertypes.flatMapTo(mutableSetOf()) { it.collectUpperBounds() }
                    else -> setOf(this)
                }

            }
            else -> return emptySet()
        }
    }

    private fun KotlinType.toTypeArgumentMapping(
        envMapping: Map<TypeParameterDescriptor, BoundTypeArgument> = emptyMap()
    ): TypeArgumentMapping? {
        val typeParameterOwner = constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return null

        val mapping = mutableMapOf<TypeParameterDescriptor, BoundTypeArgument>()
        arguments.forEachIndexed { index, typeProjection ->
            val typeParameter = typeParameterOwner.declaredTypeParameters.getOrNull(index) ?: return@forEachIndexed
            var boundTypeArgument: BoundTypeArgument = when {
                typeProjection.isStarProjection -> return@forEachIndexed
                typeProjection.projectionKind == Variance.INVARIANT -> {
                    when (typeParameter.variance) {
                        Variance.IN_VARIANCE -> BoundTypeArgument(typeProjection.type, Variance.IN_VARIANCE)
                        Variance.OUT_VARIANCE -> BoundTypeArgument(typeProjection.type, Variance.OUT_VARIANCE)
                        else -> BoundTypeArgument(typeProjection.type, Variance.INVARIANT)
                    }
                }
                else -> BoundTypeArgument(typeProjection.type, typeProjection.projectionKind)
            }

            val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(boundTypeArgument.type)
            if (typeParameterDescriptor != null) {
                val mappedTypeArgument = envMapping[typeParameterDescriptor]
                if (mappedTypeArgument != null) {
                    boundTypeArgument = mappedTypeArgument
                }
            }

            mapping.put(typeParameter, boundTypeArgument)
        }

        return TypeArgumentMapping(typeParameterOwner, mapping)
    }

    private data class TypeArgumentMapping(
        val owner: ClassifierDescriptorWithTypeParameters,
        val mapping: Map<TypeParameterDescriptor, BoundTypeArgument>
    )

    private data class BoundTypeArgument(val type: KotlinType, val variance: Variance)
    private data class BoundTypeArguments(val upper: MutableSet<KotlinType>, val lower: MutableSet<KotlinType>, val isCompatible: Boolean)

    private fun computeLeafClassesOrInterfaces(upperBoundClasses: Set<ClassDescriptor>): Set<ClassDescriptor> {
        val isLeaf = mutableMapOf<ClassDescriptor, Boolean>()
        upperBoundClasses.associateWithTo(isLeaf) { true }
        val queue = ArrayDeque(upperBoundClasses)
        while (queue.isNotEmpty()) {
            for (superClass in DescriptorUtils.getSuperclassDescriptors(queue.removeFirst())) {
                when (isLeaf[superClass]) {
                    true -> isLeaf[superClass] = false
                    false -> {}
                    else -> {
                        isLeaf[superClass] = false
                        queue.addLast(superClass)
                    }
                }
            }
        }

        return isLeaf.filterValues { it }.keys
    }

    private fun getBoundClass(type: KotlinType): ClassDescriptor? {
        return when (val declaration = type.constructor.declarationDescriptor) {
            is ClassDescriptor -> declaration
            is TypeAliasDescriptor -> getBoundClass(declaration.expandedType)
            else -> null
        }
    }

    private fun collectSuperClasses(type: KotlinType): Set<ClassDescriptor> {
        val initialClass = getBoundClass(type) ?: return emptySet()

        val result = mutableSetOf<ClassDescriptor>()
        result.add(initialClass)

        val queue = ArrayDeque<ClassDescriptor>()
        queue.addLast(initialClass)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val supertypes = DescriptorUtils.getSuperclassDescriptors(current)
            supertypes.filterNotTo(queue) { it !in result }
            result.addAll(supertypes)
        }

        return result
    }

    private fun areClassesOrInterfacesIncompatible(classesOrInterfaces: Collection<ClassDescriptor>): Boolean {
        val classes = classesOrInterfaces.filter { !it.isInterfaceLike }
        return when {
            classes.size >= 2 -> true
            !classes.any { it.isFinalOrEnum } -> false
            classesOrInterfaces.size > classes.size -> true
            else -> false
        }
    }
}

private class KtFe10BuiltinTypes(private val analysisContext: Fe10AnalysisContext) : KtBuiltinTypes() {
    override val token: ValidityToken
        get() = analysisContext.token

    override val INT: KtType
        get() = withValidityAssertion { analysisContext.builtIns.intType.toKtType(analysisContext) }

    override val LONG: KtType
        get() = withValidityAssertion { analysisContext.builtIns.longType.toKtType(analysisContext) }

    override val SHORT: KtType
        get() = withValidityAssertion { analysisContext.builtIns.shortType.toKtType(analysisContext) }

    override val BYTE: KtType
        get() = withValidityAssertion { analysisContext.builtIns.byteType.toKtType(analysisContext) }

    override val FLOAT: KtType
        get() = withValidityAssertion { analysisContext.builtIns.floatType.toKtType(analysisContext) }

    override val DOUBLE: KtType
        get() = withValidityAssertion { analysisContext.builtIns.doubleType.toKtType(analysisContext) }

    override val BOOLEAN: KtType
        get() = withValidityAssertion { analysisContext.builtIns.booleanType.toKtType(analysisContext) }

    override val CHAR: KtType
        get() = withValidityAssertion { analysisContext.builtIns.charType.toKtType(analysisContext) }

    override val STRING: KtType
        get() = withValidityAssertion { analysisContext.builtIns.stringType.toKtType(analysisContext) }

    override val UNIT: KtType
        get() = withValidityAssertion { analysisContext.builtIns.unitType.toKtType(analysisContext) }

    override val NOTHING: KtType
        get() = withValidityAssertion { analysisContext.builtIns.nothingType.toKtType(analysisContext) }

    override val ANY: KtType
        get() = withValidityAssertion { analysisContext.builtIns.anyType.toKtType(analysisContext) }

    override val THROWABLE: KtType
        get() = withValidityAssertion { analysisContext.builtIns.throwable.defaultType.toKtType(analysisContext) }

    override val NULLABLE_ANY: KtType
        get() = withValidityAssertion { analysisContext.builtIns.nullableAnyType.toKtType(analysisContext) }

    override val NULLABLE_NOTHING: KtType
        get() = withValidityAssertion { analysisContext.builtIns.nullableNothingType.toKtType(analysisContext) }

}
