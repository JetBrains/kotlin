/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.actualizer.checker.areIrExpressionConstValuesEqual
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.types.impl.IrTypeProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext.AnnotationCallInfo
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal abstract class IrExpectActualMatchingContext(
    val typeContext: IrTypeSystemContext,
    val expectToActualClassMap: Map<ClassId, IrClassSymbol>
) : ExpectActualMatchingContext<IrSymbol>, TypeSystemContext by typeContext {
    override val allowClassActualizationWithWiderVisibility: Boolean
        get() = true

    override val allowTransitiveSupertypesActualization: Boolean
        get() = true

    // This incompatibility is often suppressed in the source code (e.g. in kotlin-stdlib).
    // The backend must be able to do expect-actual matching to emit bytecode
    // That's why we disable the checker here. Probably, this checker can be enabled once KT-60426 is fixed
    override val shouldCheckDefaultParams: Boolean
        get() = false

    private inline fun <R> CallableSymbolMarker.processIr(
        onFunction: (IrFunction) -> R,
        onProperty: (IrProperty) -> R,
        onField: (IrField) -> R,
        onValueParameter: (IrValueParameter) -> R,
        onEnumEntry: (IrEnumEntry) -> R,
    ): R {
        return when (this) {
            is IrFunctionSymbol -> onFunction(owner)
            is IrPropertySymbol -> onProperty(owner)
            is IrFieldSymbol -> onField(owner)
            is IrValueParameterSymbol -> onValueParameter(owner)
            is IrEnumEntrySymbol -> onEnumEntry(owner)
            else -> error("Unsupported declaration: $this")
        }
    }

    private inline fun <R> ClassLikeSymbolMarker.processIr(
        onClass: (IrClass) -> R,
        onTypeAlias: (IrTypeAlias) -> R,
    ): R {
        return when (this) {
            is IrClassSymbol -> onClass(owner)
            is IrTypeAliasSymbol -> onTypeAlias(owner)
            else -> error("Unsupported declaration: $this")
        }
    }

    private fun DeclarationSymbolMarker.asIr(): IrDeclaration = (this as IrSymbol).owner as IrDeclaration
    private fun FunctionSymbolMarker.asIr(): IrFunction = (this as IrFunctionSymbol).owner
    private fun PropertySymbolMarker.asIr(): IrProperty = (this as IrPropertySymbol).owner
    private fun ValueParameterSymbolMarker.asIr(): IrValueParameter = (this as IrValueParameterSymbol).owner
    private fun TypeParameterSymbolMarker.asIr(): IrTypeParameter = (this as IrTypeParameterSymbol).owner
    private fun RegularClassSymbolMarker.asIr(): IrClass = (this as IrClassSymbol).owner
    private fun TypeAliasSymbolMarker.asIr(): IrTypeAlias = (this as IrTypeAliasSymbol).owner

    private inline fun <reified T : IrDeclaration> DeclarationSymbolMarker.safeAsIr(): T? = (this as IrSymbol).owner as? T

    override val innerClassesCapturesOuterTypeParameters: Boolean
        get() = false

    override val RegularClassSymbolMarker.classId: ClassId
        get() = asIr().classIdOrFail

    override val TypeAliasSymbolMarker.classId: ClassId
        get() = asIr().classIdOrFail

    override val CallableSymbolMarker.callableId: CallableId
        get() = processIr(
            onFunction = { it.callableId },
            onProperty = { it.callableId },
            onField = { it.callableId },
            onValueParameter = { shouldNotBeCalled() },
            onEnumEntry = { it.callableId }
        )

    override val TypeParameterSymbolMarker.parameterName: Name
        get() = asIr().name
    override val ValueParameterSymbolMarker.parameterName: Name
        get() = asIr().name

    override fun TypeAliasSymbolMarker.expandToRegularClass(): RegularClassSymbolMarker? {
        return asIr().expandedType.getClass()?.symbol
    }

    override val RegularClassSymbolMarker.classKind: ClassKind
        get() = asIr().kind
    override val RegularClassSymbolMarker.isCompanion: Boolean
        get() = asIr().isCompanion
    override val RegularClassSymbolMarker.isInner: Boolean
        get() = asIr().isInner
    override val RegularClassSymbolMarker.isInline: Boolean
        get() = asIr().isValue
    override val RegularClassSymbolMarker.isValue: Boolean
        get() = asIr().isValue

    override val RegularClassSymbolMarker.isFun: Boolean
        get() = asIr().isFun

    override val ClassLikeSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() {
            val parameters = processIr(
                onClass = { it.typeParameters },
                onTypeAlias = { it.typeParameters },
            )
            return parameters.map { it.symbol }
        }

    override val ClassLikeSymbolMarker.modality: Modality
        get() = processIr(
            onClass = {
                // For some reason kotlin annotations in IR have open modality and java annotations have final modality
                // But since it's legal to actualize kotlin annotation class with java annotation class
                //  and effectively all annotation classes have the same modality, it's ok to always return one
                //  modality for all annotation classes (doesn't matter final or open)
                if (it.isAnnotationClass) Modality.OPEN else it.modality
            },
            onTypeAlias = { Modality.FINAL }
        )
    override val ClassLikeSymbolMarker.visibility: Visibility
        get() = safeAsIr<IrDeclarationWithVisibility>()!!.visibility.delegate

    override val CallableSymbolMarker.modality: Modality?
        get() = when (this) {
            is IrConstructorSymbol -> Modality.FINAL
            is IrSymbol -> (owner as? IrOverridableMember)?.modality
            else -> shouldNotBeCalled()
        }

    override val CallableSymbolMarker.visibility: Visibility
        get() = when (this) {
            is IrEnumEntrySymbol -> Visibilities.Public
            is IrSymbol -> (owner as IrDeclarationWithVisibility).visibility.delegate
            else -> shouldNotBeCalled()
        }

    override val RegularClassSymbolMarker.superTypes: List<IrType>
        get() = asIr().superTypes

    override val RegularClassSymbolMarker.superTypesRefs: List<TypeRefMarker>
        get() = superTypes

    override val RegularClassSymbolMarker.defaultType: KotlinTypeMarker
        get() = asIr().defaultType

    override val CallableSymbolMarker.isExpect: Boolean
        get() = processIr(
            onFunction = { it.isExpect },
            onProperty = { it.isExpect },
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )
    override val CallableSymbolMarker.isInline: Boolean
        get() = processIr(
            onFunction = { it.isInline },
            onProperty = { false }, // property can not be inline in IR. Its getter is inline instead
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )

    override val CallableSymbolMarker.isSuspend: Boolean
        get() = processIr(
            onFunction = { it.isSuspend },
            onProperty = { false }, // property can not be suspend in IR. Its getter is suspend instead
            onField = { false },
            onValueParameter = { false },
            onEnumEntry = { false }
        )

    override val CallableSymbolMarker.isExternal: Boolean
        get() = safeAsIr<IrPossiblyExternalDeclaration>()?.isExternal ?: false

    override val CallableSymbolMarker.isInfix: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isInfix ?: false

    override val CallableSymbolMarker.isOperator: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isOperator ?: false

    override val CallableSymbolMarker.isTailrec: Boolean
        get() = safeAsIr<IrSimpleFunction>()?.isTailrec ?: false

    override val PropertySymbolMarker.isVar: Boolean
        get() = asIr().isVar

    override val PropertySymbolMarker.isLateinit: Boolean
        get() = asIr().isLateinit

    override val PropertySymbolMarker.isConst: Boolean
        get() = asIr().isConst

    override val PropertySymbolMarker.getter: FunctionSymbolMarker?
        get() = asIr().getter?.symbol

    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asIr().setter?.symbol

    override fun createExpectActualTypeParameterSubstitutor(
        expectActualTypeParameters: List<Pair<TypeParameterSymbolMarker, TypeParameterSymbolMarker>>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        val typeParametersToArguments = expectActualTypeParameters.associate { (expect, actual) ->
            (expect as IrTypeParameterSymbol) to (actual as IrTypeParameterSymbol).owner.defaultType
        }
        val substitutor = IrTypeSubstitutor(typeParametersToArguments, allowEmptySubstitution = true)
        return when (parentSubstitutor) {
            null -> substitutor
            is AbstractIrTypeSubstitutor -> IrChainedSubstitutor(parentSubstitutor, substitutor)
            else -> shouldNotBeCalled()
        }
    }

    /*
     * [isActualDeclaration] flag is needed to correctly determine scope for specific class
     * In IR there are no scopes, all declarations are stored inside IrClass itself, so this flag
     *   has no sense in IR context
     */
    override fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<DeclarationSymbolMarker> {
        return asIr().declarations.filterNot { it is IrAnonymousInitializer }.map { it.symbol }
    }

    override fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<DeclarationSymbolMarker> {
        return asIr().declarations.filter { it.getNameWithAssert() == name }.map { it.symbol }
    }

    override fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name> {
        return asIr().declarations.filterIsInstance<IrEnumEntry>().map { it.name }
    }

    override fun RegularClassSymbolMarker.collectEnumEntries(): List<DeclarationSymbolMarker> {
        return asIr().declarations.filterIsInstance<IrEnumEntry>().map { it.symbol }
    }

    override val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
        get() = (asIr().parent as? IrClass)?.defaultType

    override val CallableSymbolMarker.extensionReceiverType: IrType?
        get() = when (this) {
            is IrFunctionSymbol -> owner.extensionReceiverParameter?.type
            is IrPropertySymbol -> owner.getter?.extensionReceiverParameter?.type
            else -> null
        }

    override val CallableSymbolMarker.extensionReceiverTypeRef: TypeRefMarker?
        get() = extensionReceiverType

    override val CallableSymbolMarker.returnType: IrType
        get() = processIr(
            onFunction = { it.returnType },
            onProperty = { it.getter?.returnType ?: it.backingField?.type ?: error("No type for property: $it") },
            onField = { it.type },
            onValueParameter = { it.type },
            onEnumEntry = { it.parentAsClass.defaultType }
        )

    override val CallableSymbolMarker.returnTypeRef: TypeRefMarker
        get() = returnType


    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = processIr(
            onFunction = { it.typeParameters.map { parameter -> parameter.symbol } },
            onProperty = { it.getter?.symbol?.typeParameters.orEmpty() },
            onField = { emptyList() },
            onValueParameter = { emptyList() },
            onEnumEntry = { emptyList() }
        )

    override fun FunctionSymbolMarker.allRecursivelyOverriddenDeclarationsIncludingSelf(containingClass: RegularClassSymbolMarker?): List<CallableSymbolMarker> =
        when (val node = asIr()) {
            is IrConstructor -> listOf(this)
            is IrSimpleFunction -> (listOf(this) + node.overriddenSymbols)
                // Tests work even if you don't filter out fake-overrides. Filtering fake-overrides is needed because
                // the returned descriptors are compared by `equals`. And `equals` for fake-overrides is weird.
                // I didn't manage to invent a test that would check this condition
                .filter { !it.asIr().isFakeOverride }
            else -> error("Unknown IR node: $node")
        }

    override val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = asIr().valueParameters.map { it.symbol }

    override val ValueParameterSymbolMarker.isVararg: Boolean
        get() = asIr().isVararg
    override val ValueParameterSymbolMarker.isNoinline: Boolean
        get() = asIr().isNoinline
    override val ValueParameterSymbolMarker.isCrossinline: Boolean
        get() = asIr().isCrossinline
    override val ValueParameterSymbolMarker.hasDefaultValue: Boolean
        get() = asIr().hasDefaultValue()

    override val ValueParameterSymbolMarker.hasDefaultValueNonRecursive: Boolean
        get() = asIr().defaultValue != null

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val irConstructor = safeAsIr<IrConstructor>() ?: return false
        return irConstructor.constructedClass.isAnnotationClass
    }

    override val TypeParameterSymbolMarker.bounds: List<IrType>
        get() = asIr().superTypes
    override val TypeParameterSymbolMarker.boundsTypeRefs: List<TypeRefMarker>
        get() = bounds
    override val TypeParameterSymbolMarker.variance: Variance
        get() = asIr().variance
    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asIr().isReified

    override fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
        parameterOfAnnotationComparisonMode: Boolean,
        dynamicTypesEqualToAnything: Boolean
    ): Boolean {
        if (expectType == null) return actualType == null
        if (actualType == null) return false
        /*
         * Here we need to actualize both types, because of following situation:
         *
         *   // MODULE: common
         *   expect fun foo(): S // (1)
         *   expect class S
         *
         *   // MODULE: intermediate
         *   actual fun foo(): S = null!! // (2)
         *
         *   // MODULE: platform
         *   actual typealias S = String
         *
         * When we match return types of (1) and (2) they both will have original type `S`, but from
         *   perspective of module `platform` it should be replaced with `String`
         */
        val actualizedExpectType = expectType.actualize()
        val actualizedActualType = actualType.actualize()

        if (parameterOfAnnotationComparisonMode && actualizedExpectType is IrSimpleType && actualizedExpectType.isArray() &&
            actualizedActualType is IrSimpleType && actualizedActualType.isArray()
        ) {
            return AbstractTypeChecker.equalTypes(
                createTypeCheckerState(),
                actualizedExpectType.convertToArrayWithOutProjections(),
                actualizedActualType.convertToArrayWithOutProjections()
            )
        }

        return AbstractTypeChecker.equalTypes(
            createTypeCheckerState(),
            actualizedExpectType,
            actualizedActualType
        )
    }

    private fun IrSimpleType.convertToArrayWithOutProjections(): IrSimpleType {
        val argumentsWithOutProjection = List(arguments.size) { i ->
            val typeArgument = arguments[i]
            if (typeArgument !is IrSimpleType) typeArgument
            else makeTypeProjection(typeArgument, Variance.OUT_VARIANCE)
        }
        return IrSimpleTypeImpl(classifier, isNullable(), argumentsWithOutProjection, annotations)
    }

    private fun createTypeCheckerState(): TypeCheckerState {
        return typeContext.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = false)
    }

    override fun actualTypeIsSubtypeOfExpectType(expectType: KotlinTypeMarker, actualType: KotlinTypeMarker): Boolean {
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerState(),
            subType = actualType.actualize(),
            superType = expectType.actualize()
        )
    }

    private fun KotlinTypeMarker.actualize(): IrType {
        return actualizingSubstitutor.substitute(this as IrType)
    }

    private val actualizingSubstitutor = ActualizingSubstitutor()

    private inner class ActualizingSubstitutor : AbstractIrTypeSubstitutor() {
        override fun substitute(type: IrType): IrType {
            return substituteOrNull(type) ?: type
        }

        private fun substituteOrNull(type: IrType): IrType? {
            if (type !is IrSimpleTypeImpl) return null
            val newClassifier = (type.classifier.owner as? IrClass)?.let { expectToActualClassMap[it.classIdOrFail] }
            val newArguments = ArrayList<IrTypeArgument>(type.arguments.size)
            var argumentsChanged = false
            for (argument in type.arguments) {
                val newArgument = substituteArgumentOrNull(argument)
                if (newArgument != null) {
                    newArguments += newArgument
                    argumentsChanged = true
                } else {
                    newArguments += argument
                }
            }
            if (newClassifier == null && !argumentsChanged) {
                return null
            }
            return IrSimpleTypeImpl(
                classifier = newClassifier ?: type.classifier,
                type.nullability,
                newArguments,
                type.annotations,
                type.abbreviation
            )
        }

        private fun substituteArgumentOrNull(argument: IrTypeArgument): IrTypeArgument? {
            return when (argument) {
                is IrStarProjection -> null
                is IrTypeProjection -> when (argument) {
                    is IrTypeProjectionImpl -> {
                        val newType = substituteOrNull(argument.type) ?: return null
                        makeTypeProjection(newType, argument.variance)
                    }
                    is IrTypeBase -> substituteOrNull(argument) as IrTypeBase?
                    else -> shouldNotBeCalled()
                }
            }
        }
    }

    override fun RegularClassSymbolMarker.isNotSamInterface(): Boolean {
        /*
         * This is incorrect for java classes (because all java interfaces are considered as fun interfaces),
         *   but it's fine to not to check if some java interfaces is really SAM or not, because if one
         *   tries to actualize `expect fun interface` with typealias to non-SAM java interface, frontend
         *   will report an error and IR matching won't be invoked
         */
        return !asIr().isFun
    }

    override fun CallableSymbolMarker.isFakeOverride(containingExpectClass: RegularClassSymbolMarker?): Boolean {
        return asIr().isFakeOverride
    }

    override val CallableSymbolMarker.isDelegatedMember: Boolean
        get() = asIr().origin == IrDeclarationOrigin.DELEGATED_MEMBER

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() {
            var ir = asIr()

            if (ir.isFakeOverride && ir is IrOverridableDeclaration<*>) {
                ir.resolveFakeOverrideMaybeAbstract()?.let { ir = it }
            }

            return when (ir.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
                -> false
                else -> true
            }
        }

    override val CallableSymbolMarker.isJavaField: Boolean
        get() = this is IrFieldSymbol && owner.isFromJava()

    override fun onMatchedMembers(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {
        require(expectSymbol is IrSymbol)
        require(actualSymbol is IrSymbol)
        when (expectSymbol) {
            is IrClassSymbol -> {
                val actualClassSymbol = when (actualSymbol) {
                    is IrClassSymbol -> actualSymbol
                    is IrTypeAliasSymbol -> actualSymbol.owner.expandedType.getClass()!!.symbol
                    else -> actualSymbol.unexpectedSymbolKind<IrClassifierSymbol>()
                }
                onMatchedClasses(expectSymbol, actualClassSymbol)
            }
            else -> onMatchedCallables(expectSymbol, actualSymbol)
        }
    }

    abstract fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol)
    abstract fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol)

    override val DeclarationSymbolMarker.annotations: List<AnnotationCallInfo>
        get() = asIr().annotations.map(::AnnotationCallInfoImpl)

    override fun areAnnotationArgumentsEqual(
        expectAnnotation: AnnotationCallInfo, actualAnnotation: AnnotationCallInfo,
        collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    ): Boolean {
        fun AnnotationCallInfo.getIrElement(): IrConstructorCall = (this as AnnotationCallInfoImpl).irElement

        return areIrExpressionConstValuesEqual(
            expectAnnotation.getIrElement(),
            actualAnnotation.getIrElement(),
            collectionArgumentsCompatibilityCheckStrategy,
        )
    }

    internal fun getClassIdAfterActualization(classId: ClassId): ClassId {
        return expectToActualClassMap[classId]?.classId ?: classId
    }

    private inner class AnnotationCallInfoImpl(val irElement: IrConstructorCall) : AnnotationCallInfo {
        override val annotationSymbol: IrConstructorCall = irElement

        override val classId: ClassId?
            get() = getAnnotationClass()?.classId

        override val isRetentionSource: Boolean
            get() = getAnnotationClass()?.getAnnotationRetention() == KotlinRetention.SOURCE

        override val isOptIn: Boolean
            get() = getAnnotationClass()?.hasAnnotation(OptInNames.REQUIRES_OPT_IN_FQ_NAME) ?: false

        private fun getAnnotationClass(): IrClass? {
            val annotationClass = irElement.type.getClass() ?: return null
            return expectToActualClassMap[annotationClass.classId]?.owner ?: annotationClass
        }
    }

    override val DeclarationSymbolMarker.hasSourceAnnotationsErased: Boolean
        get() {
            val ir = asIr()
            return ir.startOffset < 0 && ir.origin !is IrDeclarationOrigin.GeneratedByPlugin
        }

    // IR checker traverses member scope itself and collects mappings
    override val checkClassScopesForAnnotationCompatibility = false

    /**
     * From IR checker point of view geter and seter are usual methods, so they don't need
     * special handling inside checker.
     * This is to prevent duplicated reports of diagnostic.
     */
    override val checkPropertyAccessorsForAnnotationsCompatibility = false

    /**
     * Same as [checkPropertyAccessorsForAnnotationsCompatibility], enum entries are usual
     * callables for IR checker, so they don't need special handling.
     */
    override val checkEnumEntriesForAnnotationsCompatibility = false

    override fun skipCheckingAnnotationsOfActualClassMember(actualMember: DeclarationSymbolMarker): Boolean = error("Should not be called")

    override fun findPotentialExpectClassMembersForActual(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
        actualMember: DeclarationSymbolMarker,
    ): Map<out DeclarationSymbolMarker, ExpectActualMatchingCompatibility> = error("Should not be called")

    // It's a stub, because not needed anywhere
    private object IrSourceElementStub : SourceElementMarker

    override fun DeclarationSymbolMarker.getSourceElement(): SourceElementMarker = IrSourceElementStub

    override fun TypeRefMarker.getClassId(): ClassId? = (this as IrType).getClass()?.classId

    override fun checkAnnotationsOnTypeRefAndArguments(
        expectContainingSymbol: DeclarationSymbolMarker,
        actualContainingSymbol: DeclarationSymbolMarker,
        expectTypeRef: TypeRefMarker,
        actualTypeRef: TypeRefMarker,
        checker: ExpectActualMatchingContext.AnnotationsCheckerCallback
    ) {
        check(expectTypeRef is IrType && actualTypeRef is IrType)

        fun IrType.getAnnotations() = annotations.map(::AnnotationCallInfoImpl)

        checker.check(expectTypeRef.getAnnotations(), actualTypeRef.getAnnotations(), IrSourceElementStub)

        if (expectTypeRef !is IrSimpleType || actualTypeRef !is IrSimpleType) return
        if (expectTypeRef.arguments.size != actualTypeRef.arguments.size) return

        for ((expectArg, actualArg) in expectTypeRef.arguments.zip(actualTypeRef.arguments)) {
            val expectArgType = expectArg.typeOrNull ?: continue
            val actualArgType = actualArg.typeOrNull ?: continue
            checkAnnotationsOnTypeRefAndArguments(expectContainingSymbol, actualContainingSymbol, expectArgType, actualArgType, checker)
        }
    }
}
