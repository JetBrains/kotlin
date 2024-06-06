/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.actualizer.ClassActualizationInfo
import org.jetbrains.kotlin.backend.common.actualizer.SpecialFakeOverrideSymbolsResolver
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.isDelegated
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.implicitCastTo
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.EnhancedNullability
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FlexibleNullability
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class Fir2IrFakeOverrideStrategy(
    friendModules: Map<String, List<String>>,
    override val isGenericClashFromSameSupertypeAllowed: Boolean,
    override val isOverrideOfPublishedApiFromOtherModuleDisallowed: Boolean,
    delegatedMemberGenerationStrategy: Fir2IrDelegatedMembersGenerationStrategy,
) : FakeOverrideBuilderStrategy.BindToPrivateSymbols(friendModules, delegatedMemberGenerationStrategy) {
    private val fieldOnlyProperties: MutableList<IrPropertyWithLateBinding> = mutableListOf()

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
        super.linkPropertyFakeOverride(property, manglerCompatibleMode)

        if (property.getter == null) {
            fieldOnlyProperties.add(property)
        }
    }

    fun clearFakeOverrideFields() {
        for (property in fieldOnlyProperties) {
            check(property.isFakeOverride && property.getter == null) { "Not a field-only property: " + property.render() }
            property.backingField = null
        }
    }
}

private data class DelegatedMemberInfo(
    val delegatedMember: IrOverridableDeclaration<*>,
    val delegateTargetFromBaseType: IrOverridableDeclaration<*>,
    val classSymbolOfDelegateField: IrClassSymbol,
    val delegateField: IrField,
    val parent: IrClass,
)

/**
 * Generation of delegated members happens in three stages:
 * 1. During f/o generation [postProcessGeneratedFakeOverride] is called. This method updates the declaration header if needed
 *     (offsets, origin, dispatch receiver parameter and so on) and stores information for body generation
 * 2. After all f/o are built [generateDelegatedBodies] should be called to generate bodies for all delegated members.
 *     Body generation relies on the fact that all f/o are already created, because it may refer to types of functions, which are
 *     actually called for delegation.
 * 3. [updateMetadataSources] is called after all expect/actual pairs are matched to set the metadata source for delegated members
 *
 *
 * [delegatedClassesInfo] is a map, which contains for each class, which implements inheritance by delegation (key) mapping between
 *   delegated super interface and the field, which contains corresponding value with delegate
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class Fir2IrDelegatedMembersGenerationStrategy(
    private val irFactory: IrFactory,
    private val irBuiltins: IrBuiltIns,
    private val fir2IrExtensions: Fir2IrExtensions,
    delegatedClassesInfo: Map<IrClassSymbol, Map<IrClassSymbol, IrFieldSymbol>>,
    classActualizationInfo: ClassActualizationInfo?
) : IrUnimplementedOverridesStrategy {
    private val delegatedInfos: MutableList<DelegatedMemberInfo> = mutableListOf()

    /**
     * In MPP we need to actualize class symbols in super-interfaces map
     */
    private val delegatedClassesInfo: Map<IrClassSymbol, Map<IrClassSymbol, IrFieldSymbol>> = when (classActualizationInfo) {
        null -> delegatedClassesInfo
        else -> delegatedClassesInfo.mapValues { (_, map) ->
            map.mapKeys { (classSymbol, _) ->
                val classId = classSymbol.owner.classId ?: return@mapKeys classSymbol
                val actualizedDeclaration = classActualizationInfo.getActualWithoutExpansion(classId)?.owner ?: return@mapKeys classSymbol
                when (actualizedDeclaration) {
                    is IrClass -> actualizedDeclaration.symbol
                    is IrTypeAlias -> actualizedDeclaration.expandedType.classOrFail
                    else -> error("Unexpected declaration: ${actualizedDeclaration.render()}")
                }
            }
        }
    }

    override fun <S : IrSymbol, T : IrOverridableDeclaration<S>> computeCustomization(
        overridableMember: T,
        parent: IrClass,
    ): IrUnimplementedOverridesStrategy.Customization {
        return IrUnimplementedOverridesStrategy.Customization.NO
    }

    override fun <S : IrSymbol, T : IrOverridableDeclaration<S>> postProcessGeneratedFakeOverride(overridableMember: T, parent: IrClass) {
        val delegateInfo = delegatedClassesInfo[parent.symbol] ?: return
        if (!fir2IrExtensions.shouldGenerateDelegatedMember(overridableMember)) return

        val overridden = overridableMember.allOverridden()
        val matched = overridden.mapNotNull {
            if (it is IrSimpleFunction && it.isFakeOverriddenFromAny()) return@mapNotNull null
            val matchedField = delegateInfo[it.parentAsClass.symbol] ?: return@mapNotNull null
            it to matchedField
        }
        when (matched.size) {
            0 -> return
            1 -> {}
            else -> {
                errorWithAttachment("Too many suitable delegated supertypes for single delegated declaration") {
                    withEntry("delegated declaration", overridableMember.render())
                    matched.forEach { (supertypeMember, delegateField) ->
                        withEntryGroup("matched delegate") {
                            withEntry("delegate field", delegateField.owner.render())
                            withEntry("supertype member", supertypeMember.render())
                        }
                    }
                }
            }
        }
        val (delegateTargetFromBaseType, delegateFieldSymbol) = matched.single()
        val delegateField = delegateFieldSymbol.owner

        fun IrType.extractClassSymbol(): IrClassSymbol {
            return when (val classifier = this.classifierOrFail) {
                is IrClassSymbol -> classifier
                is IrTypeParameterSymbol -> classifier.owner.superTypes.first().extractClassSymbol()
                else -> shouldNotBeCalled()
            }
        }

        val classOfDelegateField = delegateField.type.extractClassSymbol()

        when (overridableMember) {
            is IrSimpleFunction -> overridableMember.updateDeclarationHeader()
            is IrProperty -> {
                overridableMember.updateDeclarationHeader()
                overridableMember.getter?.updateDeclarationHeader()
                overridableMember.setter?.updateDeclarationHeader()
            }
        }

        delegatedInfos += DelegatedMemberInfo(
            overridableMember,
            delegateTargetFromBaseType,
            classOfDelegateField,
            delegateField,
            parent
        )
    }

    private fun IrOverridableDeclaration<*>.updateDeclarationHeader() {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        isFakeOverride = false
        origin = IrDeclarationOrigin.DELEGATED_MEMBER
        modality = Modality.OPEN
        if (this is IrSimpleFunction) {
            dispatchReceiverParameter = null
            createDispatchReceiverParameter()
        }
    }

    fun generateDelegatedBodies() {
        for (delegatedInfo in delegatedInfos) {
            val (delegatedMember, delegateTargetFromBaseType, classSymbolOfDelegateField, delegateField, parent) = delegatedInfo
            when (delegatedMember) {
                is IrSimpleFunction -> generateDelegatedFunctionBody(
                    delegatedMember,
                    delegateTargetFromBaseType as IrSimpleFunction,
                    classSymbolOfDelegateField,
                    delegateField,
                    parent,
                    Kind.Function
                )
                is IrProperty -> generateDelegatedPropertyBody(
                    delegatedMember,
                    delegateTargetFromBaseType as IrProperty,
                    classSymbolOfDelegateField,
                    delegateField,
                    parent
                )
                else -> error("Unexpected member kind: ${delegatedMember::class.qualifiedName}")
            }
        }
    }

    /**
     * To update the metadata source, we need to know FIR <-> IR mapping for delegated declarations. Getting this mapping is not trivial,
     *   as delegated members are built using only IR. So to achieve it we do the following things:
     * - iterate through all delegated members in FIR (using session of platform module)
     * - for each such member compute fake-override symbol using mechanism from fir2ir part
     * - map this symbol with symbol of real generated declaration
     * - set the source metadata for IR under this symbol
     */
    fun updateMetadataSources(
        delegatedClasses: Collection<FirClass>,
        session: FirSession,
        scopeSession: ScopeSession,
        declarationStorage: Fir2IrDeclarationStorage,
        symbolResolver: SpecialFakeOverrideSymbolsResolver
    ) {
        for (firClass in delegatedClasses) {
            val classLookupTag = firClass.symbol.toLookupTag()
            val scope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
            scope.processAllFunctions l@{ firSymbol ->
                if (!firSymbol.isDelegated) return@l
                if (firSymbol.dispatchReceiverClassLookupTagOrNull() != classLookupTag) return@l
                val fakeOverrideIrSymbol = declarationStorage.getIrFunctionSymbol(
                    firSymbol.unwrapFakeOverridesOrDelegated(),
                    classLookupTag
                )
                val delegatedIrFunction = symbolResolver.getReferencedFunction(fakeOverrideIrSymbol).owner
                @OptIn(SymbolInternals::class)
                delegatedIrFunction.metadata = FirMetadataSource.Function(firSymbol.fir)
            }
            scope.processAllProperties l@{ firSymbol ->
                if (!firSymbol.isDelegated) return@l
                if (firSymbol.dispatchReceiverClassLookupTagOrNull() != classLookupTag) return@l
                if (firSymbol !is FirPropertySymbol) return@l
                val fakeOverrideIrSymbol = declarationStorage.getIrPropertySymbol(
                    firSymbol.unwrapFakeOverridesOrDelegated(),
                    classLookupTag
                ) as IrPropertySymbol? ?: return@l
                val delegatedIrProperty = symbolResolver.getReferencedProperty(fakeOverrideIrSymbol).owner
                @OptIn(SymbolInternals::class)
                delegatedIrProperty.metadata = FirMetadataSource.Property(firSymbol.fir)
            }
        }
    }

    private enum class Kind {
        Function, Getter, Setter
    }

    private fun generateDelegatedPropertyBody(
        delegatedProperty: IrProperty,
        delegateTargetFromBaseType: IrProperty,
        classSymbolOfDelegateField: IrClassSymbol,
        delegateField: IrField,
        parent: IrClass,
    ) {
        delegatedProperty.annotations = emptyList()
        delegatedProperty.getter?.let {
            generateDelegatedFunctionBody(
                it, delegateTargetFromBaseType.getter!!, classSymbolOfDelegateField,
                delegateField, parent, Kind.Getter
            )
        }
        delegatedProperty.setter?.let {
            generateDelegatedFunctionBody(
                it, delegateTargetFromBaseType.setter!!, classSymbolOfDelegateField,
                delegateField, parent, Kind.Setter
            )
        }
    }

    /**
     * ```
     * interface Base {
     *     fun foo() // <---- [delegateTargetFromBaseType]
     * }
     *
     * class Impl : Base() {
     *     override fun foo() {}
     * }
     *
     * class Delegated_1 : Base by Impl() { // <---- [parent]
     *     field delegate$1 = Impl() // <---- [delegateField]
     *
     *     delegated fun foo() { ... } // <---- [delegatedFunction]
     *
     *     // [classSymbolOfDelegateField] = Impl
     * }
     *
     * class Delegated_2(val b: Base) : Base by b { // <---- [parent]
     *     backing_field_of_b // <---- [delegateField]
     *
     *     delegated fun foo() { ... } // <---- [delegatedFunction]
     *
     *     // [classSymbolOfDelegateField] = Base
     * }
     * ```
     *
     */
    private fun generateDelegatedFunctionBody(
        delegatedFunction: IrSimpleFunction,
        delegateTargetFromBaseType: IrSimpleFunction,
        classSymbolOfDelegateField: IrClassSymbol,
        delegateField: IrField,
        parent: IrClass,
        kind: Kind
    ) {
        val (delegateTargetFunction, substitutor, delegatingToMethodOfSupertype) = extractDelegatedFunctionBodyInfo(
            classSymbolOfDelegateField,
            delegateTargetFromBaseType,
            parent,
            kind,
            delegatedFunction,
            delegateField
        )

        val offset = SYNTHETIC_OFFSET

        val substitutorForReturnType = when {
            delegatedFunction.typeParameters.isEmpty() -> substitutor
            else -> IrChainedSubstitutor(
                IrTypeSubstitutor(
                    delegateTargetFunction.typeParameters.map { it.symbol },
                    delegatedFunction.typeParameters.map { it.defaultType },
                    allowEmptySubstitution = true,
                ),
                substitutor
            )
        }
        val callReturnType = substitutorForReturnType.substitute(delegateTargetFunction.returnType)

        val irCall = IrCallImpl(
            offset,
            offset,
            callReturnType,
            delegateTargetFunction.symbol,
            delegatedFunction.typeParameters.size,
            delegatedFunction.valueParameters.size
        ).apply {
            val thisDispatchReceiverParameter = delegatedFunction.dispatchReceiverParameter!!
            val getField = IrGetFieldImpl(
                offset, offset,
                delegateField.symbol,
                delegateField.type,
                IrGetValueImpl(
                    offset, offset,
                    thisDispatchReceiverParameter.type,
                    thisDispatchReceiverParameter.symbol
                )
            ).let {
                /**
                 * If a type of the field is not a subtype of interface which is delegated, we need to add implicit cast to get field
                 * Such a situation may occur in case when delegate field has intersection type
                 * ```
                 * val some: A & B = select(C, D)
                 * val obj = object : A by some
                 * ```
                 */
                if (!delegatingToMethodOfSupertype) return@let it
                val baseType = substitutor.substitute(delegateTargetFromBaseType.parentAsClass.defaultType)
                if (delegateField.type.isSubtypeOfClass(baseType.classOrFail)) return@let it
                it.implicitCastTo(baseType)
            }
            dispatchReceiver = getField
            extensionReceiver = delegatedFunction.extensionReceiverParameter?.let { extensionReceiver ->
                IrGetValueImpl(offset, offset, extensionReceiver.type, extensionReceiver.symbol)
            }
            delegatedFunction.valueParameters.forEach {
                putValueArgument(it.index, IrGetValueImpl(offset, offset, it.type, it.symbol))
            }
            for (index in delegatedFunction.typeParameters.indices) {
                val parameter = delegatedFunction.typeParameters[index]
                putTypeArgument(
                    index, IrSimpleTypeImpl(
                        parameter.symbol,
                        hasQuestionMark = false,
                        arguments = emptyList(),
                        annotations = emptyList()
                    )
                )
            }
        }

        val irCastOrCall = if (
            delegateTargetFunction.returnType.let { it.hasAnnotation(FlexibleNullability) || it.hasAnnotation(EnhancedNullability) } &&
            !delegatedFunction.returnType.isMarkedNullable()
        ) {
            Fir2IrImplicitCastInserter.implicitNotNullCast(irCall)
        } else {
            irCall
        }

        val body = irFactory.createBlockBody(offset, offset)
        if (delegatedFunction.isSetter || delegatedFunction.returnType.isUnit() || delegatedFunction.returnType.isNothing()) {
            body.statements.add(irCastOrCall)
        } else {
            val irReturn = IrReturnImpl(offset, offset, irBuiltins.nothingType, delegatedFunction.symbol, irCastOrCall)
            body.statements.add(irReturn)
        }

        delegatedFunction.body = body
    }

    private data class DelegatedFunctionBodyInfo(
        val delegateTargetFunction: IrSimpleFunction,
        val substitutor: AbstractIrTypeSubstitutor,
        val delegatingToMethodOfSupertype: Boolean,
    )

    private fun extractDelegatedFunctionBodyInfo(
        classSymbolOfDelegateField: IrClassSymbol,
        delegateTargetFromBaseType: IrSimpleFunction,
        parent: IrClass,
        kind: Kind,
        delegatedFunction: IrSimpleFunction,
        delegateField: IrField,
    ): DelegatedFunctionBodyInfo {
        val targetFunctionFromDelegateFieldClass = classSymbolOfDelegateField.owner.declarations
            .firstNotNullOfOrNull l@{ declaration ->
                val function = when (kind) {
                    Kind.Function -> declaration as? IrSimpleFunction ?: return@l null
                    Kind.Getter -> (declaration as? IrProperty)?.getter ?: return@l null
                    Kind.Setter -> (declaration as? IrProperty)?.setter ?: return@l null
                }
                function.takeIf { it.overrides(delegateTargetFromBaseType) }
            }
        val typeParametersMatch =
            targetFunctionFromDelegateFieldClass?.typeParameters?.size == delegatedFunction.typeParameters.size

        if (!typeParametersMatch) {
            // fallback to delegation to interface member
            return DelegatedFunctionBodyInfo(
                delegateTargetFunction = delegateTargetFromBaseType,
                substitutor = createSupertypeSubstitutor(parentClass = delegateTargetFromBaseType.parentAsClass, parent.defaultType),
                delegatingToMethodOfSupertype = true,
            )
        }

        requireNotNull(targetFunctionFromDelegateFieldClass)
        require(classSymbolOfDelegateField == targetFunctionFromDelegateFieldClass.parentAsClass.symbol)

        return DelegatedFunctionBodyInfo(
            targetFunctionFromDelegateFieldClass,
            substitutor = IrTypeSubstitutor(
                classSymbolOfDelegateField.owner.typeParameters.map { it.symbol },
                (delegateField.type as IrSimpleType).arguments,
                allowEmptySubstitution = true
            ),
            delegatingToMethodOfSupertype = false
        )
    }
}

private fun createSupertypeSubstitutor(parentClass: IrClass, type: IrSimpleType): AbstractIrTypeSubstitutor {
    val visited = mutableSetOf<IrClass>()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun find(type: IrSimpleType, targetClass: IrClass): AbstractIrTypeSubstitutor? {
        val currentClass = type.classOrFail.owner
        if (currentClass == targetClass) return AbstractIrTypeSubstitutor.Empty
        if (!visited.add(currentClass)) return null

        val superTypeSubstitutors = getImmediateSupertypes(type.classOrFail.owner)

        superTypeSubstitutors.firstNotNullOfOrNull {
            runIf(it.key.classOrFail.owner == targetClass) { it.value }
        }?.let { return it }

        for ((superType, substitutor) in superTypeSubstitutors) {
            val otherSubstitutor = find(superType, targetClass) ?: continue
            return IrChainedSubstitutor(substitutor, otherSubstitutor)
        }
        return null
    }

    return find(type, parentClass) ?: error("Supertype substitutor not found")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun getImmediateSupertypes(irClass: IrClass): Map<IrSimpleType, AbstractIrTypeSubstitutor> {
    @Suppress("UNCHECKED_CAST")
    val originalSupertypes = irClass.superTypes as List<IrSimpleType>
    return originalSupertypes
        .filter { it.classOrNull != null }
        .associateWith { superType ->
            val superClass = superType.classOrFail.owner
            IrTypeSubstitutor(superClass.typeParameters.map { it.symbol }, superType.arguments, allowEmptySubstitution = true)
        }
}
