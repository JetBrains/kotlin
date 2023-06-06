/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
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
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal abstract class IrExpectActualMatchingContext(
    val typeContext: IrTypeSystemContext,
    val expectToActualClassMap: Map<ClassId, IrClassSymbol>
) : ExpectActualMatchingContext<IrSymbol>, TypeSystemContext by typeContext {
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

    override val shouldCheckReturnTypesOfCallables: Boolean
        get() = true

    override val innerClassesCapturesOuterTypeParameters: Boolean
        get() = false

    override val enumConstructorsAreAlwaysCompatible: Boolean
        get() = true

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

    override val RegularClassSymbolMarker.superTypes: List<KotlinTypeMarker>
        get() = asIr().superTypes

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

    override val PropertySymbolMarker.setter: FunctionSymbolMarker?
        get() = asIr().setter?.symbol

    @OptIn(UnsafeCastFunction::class)
    override fun createExpectActualTypeParameterSubstitutor(
        expectTypeParameters: List<TypeParameterSymbolMarker>,
        actualTypeParameters: List<TypeParameterSymbolMarker>,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): TypeSubstitutorMarker {
        val expectParameters = expectTypeParameters.castAll<IrTypeParameterSymbol>()
        val actualParameters = actualTypeParameters.castAll<IrTypeParameterSymbol>()
        val actualTypes = actualParameters.map { it.owner.defaultType }
        val substitutor = IrTypeSubstitutor(expectParameters, actualTypes, typeContext.irBuiltIns, allowEmptySubstitution = true)
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

    override val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
        get() = (asIr().parent as? IrClass)?.defaultType

    override val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
        get() = safeAsIr<IrFunction>()?.extensionReceiverParameter?.type

    override val CallableSymbolMarker.returnType: KotlinTypeMarker
        get() = processIr(
            onFunction = { it.returnType },
            onProperty = { it.getter?.returnType ?: it.backingField?.type ?: error("No type for property: $it") },
            onField = { it.type },
            onValueParameter = { it.type },
            onEnumEntry = { it.parentAsClass.defaultType }
        )

    override val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
        get() = processIr(
            onFunction = { it.typeParameters.map { parameter -> parameter.symbol } },
            onProperty = { it.getter?.symbol?.typeParameters.orEmpty() },
            onField = { emptyList() },
            onValueParameter = { emptyList() },
            onEnumEntry = { emptyList() }
        )

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

    override fun CallableSymbolMarker.isAnnotationConstructor(): Boolean {
        val irConstructor = safeAsIr<IrConstructor>() ?: return false
        return irConstructor.constructedClass.isAnnotationClass
    }

    override val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
        get() = asIr().superTypes
    override val TypeParameterSymbolMarker.variance: Variance
        get() = asIr().variance
    override val TypeParameterSymbolMarker.isReified: Boolean
        get() = asIr().isReified

    override fun areCompatibleExpectActualTypes(expectType: KotlinTypeMarker?, actualType: KotlinTypeMarker?): Boolean {
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
        val actualizedExpectType = actualizingSubstitutor.substitute(expectType as IrType)
        val actualizedActualType = actualizingSubstitutor.substitute(actualType as IrType)
        return AbstractTypeChecker.equalTypes(
            typeContext.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = false),
            actualizedExpectType,
            actualizedActualType
        )
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

    override fun CallableSymbolMarker.shouldSkipMatching(containingExpectClass: RegularClassSymbolMarker): Boolean {
        return false
    }

    override val CallableSymbolMarker.hasStableParameterNames: Boolean
        get() = when (asIr().origin) {
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
            -> false
            else -> true
        }

    override fun onMatchedMembers(expectSymbol: DeclarationSymbolMarker, actualSymbol: DeclarationSymbolMarker) {
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
}
