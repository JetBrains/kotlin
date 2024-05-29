/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.symbols.LateBoundBirSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSymbolBase
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrCapturedType
import org.jetbrains.kotlin.ir.types.impl.IrDelegatedSimpleType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeProjectionImpl
import java.util.*
import kotlin.collections.set

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class Ir2BirConverterBase(
    override val compressedSourceSpanManager: CompressedSourceSpanManager,
) : CompressedSourceSpanManagerScope {
    var elementConverted: (IrElement, BirElement) -> Unit = { _, _ -> }
    var convertAncestorsForOrphanedElements = false
    var instantiateDescriptors = false
    var recordConvertedTypes = false
    var convertLazyElementsIntoImpl = false
    var convertImplElementsIntoLazyWhenPossible = false

    private val collectedBirElementsWithoutParent = mutableListOf<BirElement>()
    private val collectedIrElementsWithoutParent = mutableListOf<IrElement>()
    private var isInsideNestedElementCopy = false
    private var isInsideCopyAncestorsForOrphanedElements = false

    private val symbolOwnersCurrentlyBeingConverted = IdentityHashMap<IrSymbolOwner, LateBoundBirSymbol<*>?>()
    val remappedIr2BirTypes = IdentityHashMap<BirType, IrType>()

    val currentColBirElementsWithoutParent: List<BirElement>
        get() = collectedBirElementsWithoutParent

    protected fun <Bir : BirElement, Ir : IrElement> createElementMap(expectedMaxSize: Int = 8): MutableMap<Ir, Bir> =
        IdentityHashMap<Ir, Bir>(expectedMaxSize)

    protected abstract fun <Bir : BirElement> copyElement(old: IrElement): Bir
    protected abstract fun <Bir : BirElement> copyLazyElement(old: IrDeclaration): Bir?

    fun copyIrTree(irRootElements: List<IrElement>): List<BirElement> {
        return irRootElements.map { copyElement(it) }
    }

    fun copyIrTree(irRootElement: IrElement): BirElement =
        copyIrTree(listOf(irRootElement)).single()

    protected fun <Ir : IrElement, Bir : BirElement> copyNotReferencedElement(old: Ir, copy: () -> Bir): Bir {
        val new = doCopyElement(old, copy)
        elementConverted(old, new)
        return new
    }

    protected fun <Ir : IrElement, ME : BirElement, SE : ME> copyReferencedElement(
        old: Ir,
        map: MutableMap<Ir, ME>,
        copy: () -> SE,
        lateInitialize: (SE) -> Unit,
    ): SE {
        map[old]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as SE
        }

        if (old is IrDeclaration && (convertImplElementsIntoLazyWhenPossible || !convertLazyElementsIntoImpl && old is IrLazyDeclarationBase)) {
            copyLazyElement<SE>(old)?.let { new ->
                map[old] = new

                // IrProperty.getter/setter/backingField is inconsistent -
                // it is a child of IrProperty, but its parent is often a class
                val irParent = when (old) {
                    is IrField -> old.correspondingPropertySymbol
                    is IrSimpleFunction -> old.correspondingPropertySymbol
                    else -> null
                }?.owner ?: old.parent

                val birParent = remapElement<BirDeclarationParent>(irParent)
                fixExternalPackegeBeingElementsParent(new, irParent, birParent)
                (new as BirLazyElementBase).initParent(birParent as BirElementBase)

                elementConverted(old, new)
                return new
            }
        }

        return doCopyElement(old) {
            if (old is IrSymbolOwner) {
                symbolOwnersCurrentlyBeingConverted[old] = null
            }

            val new = copy()
            map[old] = new

            elementConverted(old, new)

            if (old is IrSymbolOwner) {
                val symbol = symbolOwnersCurrentlyBeingConverted.remove(old)
                if (symbol != null) {
                    @Suppress("UNCHECKED_CAST")
                    (symbol as LateBoundBirSymbol<BirSymbolOwner>).bind(new as BirSymbolOwner)
                }
            }

            lateInitialize(new)
            new
        }
    }

    private fun <Ir : IrElement, Bir : BirElement> doCopyElement(old: Ir, copy: () -> Bir): Bir {
        val isNested = isInsideNestedElementCopy
        isInsideNestedElementCopy = true
        val lastCollectedElementsWithoutParent = collectedBirElementsWithoutParent.size
        val new = copy()

        if (isNested) {
            for (i in collectedBirElementsWithoutParent.lastIndex downTo lastCollectedElementsWithoutParent) {
                val bir = collectedBirElementsWithoutParent[i]
                if (bir.parent != null) {
                    collectedBirElementsWithoutParent.removeAt(i)
                    collectedIrElementsWithoutParent.removeAt(i)
                }
            }
        }

        if (new.parent == null) {
            if (old is IrDeclaration && old !is IrModuleFragment && old !is IrExternalPackageFragment || old is IrFile) {
                collectedBirElementsWithoutParent += new
                collectedIrElementsWithoutParent += old
            }
        }

        if (!isNested) {
            if (convertAncestorsForOrphanedElements || isInsideCopyAncestorsForOrphanedElements) {
                doCopyAncestorsForCollectedOrphanedElements()
            }
        }

        isInsideNestedElementCopy = isNested
        return new
    }

    private fun doCopyAncestorsForCollectedOrphanedElements() {
        while (true) {
            val bir = collectedBirElementsWithoutParent.removeLastOrNull() ?: break
            val ir = collectedIrElementsWithoutParent.removeLast()
            if (bir.parent == null) {
                if (ir is IrDeclaration) {
                    val irParent = ir.parent
                    val birParent = remapElement<BirElement>(irParent)

                    if (bir.parent == null) {
                        fixExternalPackegeBeingElementsParent(bir, irParent, birParent)
                    }
                } else if (ir is IrFile) {
                    remapElement<BirModuleFragment>(ir.module)
                }
            }
        }
    }

    private fun fixExternalPackegeBeingElementsParent(element: BirElement, irParent: IrElement, birParent: BirElement) {
        // Just creating the BIR element for the parent should automatically
        // add register this element as its child.
        // However, IrExternalPackageFragments do not always contain their children.
        if (irParent is IrExternalPackageFragment) {
            birParent as BirExternalPackageFragment
            birParent.declarations += element as BirDeclaration
        }
    }

    fun copyAncestorsForCollectedOrphanedElements() {
        isInsideCopyAncestorsForOrphanedElements = true
        doCopyAncestorsForCollectedOrphanedElements()
        isInsideCopyAncestorsForOrphanedElements = false
    }

    fun <Bir : BirElement> remapElement(old: IrElement): Bir = copyElement(old)

    @JvmName("remapElementNullable")
    fun <Bir : BirElement> remapElement(old: IrElement?): Bir? = if (old == null) null else copyElement(old)

    fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(old: IrS): BirS {
        check(old.isBound) { "All symbols should be bound at this stage" }
        val owner = old.owner
        return if (symbolOwnersCurrentlyBeingConverted.isNotEmpty() && owner in symbolOwnersCurrentlyBeingConverted) {
            @Suppress("UNCHECKED_CAST")
            symbolOwnersCurrentlyBeingConverted.computeIfAbsent(owner) { convertLateBindSymbol(old) } as BirS
        } else {
            remapElement(owner) as BirS
        }
    }

    @JvmName("remapSymbolNullable")
    fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(old: IrS?): BirS? = if (old == null) null else remapSymbol(old)

    private fun <BirS : LateBoundBirSymbol<*>, IrS : IrSymbol> convertLateBindSymbol(old: IrS): BirS {
        val signature = old.signature
        @Suppress("UNCHECKED_CAST")
        return when (old) {
            is IrFileSymbol -> LateBoundBirSymbol.FileSymbol(signature)
            is IrExternalPackageFragmentSymbol -> LateBoundBirSymbol.ExternalPackageFragmentSymbol(signature)
            is IrAnonymousInitializerSymbol -> LateBoundBirSymbol.AnonymousInitializerSymbol(signature)
            is IrEnumEntrySymbol -> LateBoundBirSymbol.EnumEntrySymbol(signature)
            is IrFieldSymbol -> LateBoundBirSymbol.FieldSymbol(signature)
            is IrClassSymbol -> LateBoundBirSymbol.ClassSymbol(signature)
            is IrScriptSymbol -> LateBoundBirSymbol.ScriptSymbol(signature)
            is IrTypeParameterSymbol -> LateBoundBirSymbol.TypeParameterSymbol(signature)
            is IrValueParameterSymbol -> LateBoundBirSymbol.ValueParameterSymbol(signature)
            is IrVariableSymbol -> LateBoundBirSymbol.VariableSymbol(signature)
            is IrConstructorSymbol -> LateBoundBirSymbol.ConstructorSymbol(signature)
            is IrSimpleFunctionSymbol -> LateBoundBirSymbol.SimpleFunctionSymbol(signature)
            is IrReturnableBlockSymbol -> LateBoundBirSymbol.ReturnableBlockSymbol(signature)
            is IrPropertySymbol -> LateBoundBirSymbol.PropertySymbol(signature)
            is IrLocalDelegatedPropertySymbol -> LateBoundBirSymbol.LocalDelegatedPropertySymbol(signature)
            is IrTypeAliasSymbol -> LateBoundBirSymbol.TypeAliasSymbol(signature)
            else -> error(old)
        } as BirS
    }

    protected fun BirAttributeContainer.copyAttributes(old: IrAttributeContainer) {
        val owner = old.attributeOwnerId
        attributeOwnerId = if (owner === old) this else remapElement(owner)
    }

    protected fun <Ir : IrElement, Bir : BirElement> BirImplChildElementList<Bir>.copyElements(from: List<Ir>) {
        ensureCapacity(from.size)
        for (ir in from) {
            val bir = copyElement<Bir>(ir)
            this += bir
        }
    }

    protected fun BirMemberAccessExpression<*>.copyIrMemberAccessExpressionValueArguments(from: IrMemberAccessExpression<*>) {
        valueArguments.resetWithNulls(from.valueArgumentsCount)
        for (i in 0 until from.valueArgumentsCount) {
            val arg = from.getValueArgument(i)
            if (arg != null) {
                valueArguments[i] = copyElement(arg) as BirExpression
            }
        }
    }

    protected val IrMemberAccessExpression<*>.typeArguments: List<IrType?>
        get() = List(typeArgumentsCount) { getTypeArgument(it) }

    protected fun <D : DeclarationDescriptor> mapDescriptor(readDescriptor: () -> D?): D? {
        return if (instantiateDescriptors) readDescriptor() else null
    }

    protected fun <D : DeclarationDescriptor> mapDescriptor(declaration: IrSymbolOwner): D? {
        val symbol = declaration.symbol
        return if (symbol is IrSymbolBase<*, *> && (instantiateDescriptors || symbol.hasDescriptor)) {
            @Suppress("UNCHECKED_CAST")
            symbol.descriptor as D
        } else null
    }

    fun remapType(irType: IrType): BirType = when (irType) {
        // for IrDelegatedSimpleType, this egaerly initializes a lazy IrAnnotationType
        is IrCapturedType -> remapCapturedType(irType)
        is IrSimpleTypeImpl, is IrDelegatedSimpleType -> remapSimpleTypeImpl(irType as IrSimpleType)
        is IrDynamicType -> remapDynamicType(irType)
        is IrErrorType -> remapErrorType(irType)
        else -> TODO(irType.toString())
    }.also {
        if (recordConvertedTypes) {
            remappedIr2BirTypes[it] = irType
        }
    }

    @JvmName("remapTypeNullable")
    fun remapType(irType: IrType?): BirType? = if (irType == null) null else remapType(irType)

    fun remapSimpleType(irType: IrSimpleType) = remapType(irType) as BirSimpleType

    private fun remapSimpleTypeImpl(irType: IrSimpleType): BirSimpleType {
        return BirSimpleTypeImpl(
            irType.kotlinType,
            remapSymbol(irType.classifier),
            irType.nullability,
            irType.arguments.map { remapTypeArgument(it) },
            irType.annotations.map { remapElement(it) as BirConstructorCall },
            irType.abbreviation?.let { abbreviation ->
                remapTypeAbbreviation(abbreviation)
            },
        )
    }

    private fun remapTypeAbbreviation(abbreviation: IrTypeAbbreviation): BirTypeAbbreviation {
        return BirTypeAbbreviation(
            remapSymbol(abbreviation.typeAlias),
            abbreviation.hasQuestionMark,
            abbreviation.arguments.map { remapTypeArgument(it) },
            abbreviation.annotations.map { remapElement(it) as BirConstructorCall },
        )
    }

    private fun remapCapturedType(irType: IrCapturedType): BirCapturedType {
        return BirCapturedType(
            irType.captureStatus,
            irType.lowerType?.let { remapType(it) },
            remapTypeArgument(irType.constructor.argument),
            remapElement(irType.constructor.typeParameter) as BirTypeParameter,
            irType.nullability,
            irType.annotations.map { remapElement(it) as BirConstructorCall },
            irType.abbreviation?.let { remapTypeAbbreviation(it) },
        )
    }

    private fun remapDynamicType(irType: IrDynamicType): BirDynamicType {
        return BirDynamicType(
            irType.kotlinType,
            irType.annotations.map { remapElement(it) as BirConstructorCall },
            irType.variance,
        )
    }

    private fun Ir2BirConverterBase.remapErrorType(irType: IrErrorType) =
        BirErrorType(
            irType.kotlinType,
            irType.annotations.map { remapElement(it) as BirConstructorCall },
            irType.variance,
            irType.isMarkedNullable,
        )

    private fun remapTypeArgument(irTypeArgument: IrTypeArgument): BirTypeArgument = when (irTypeArgument) {
        is IrStarProjection -> BirStarProjection
        is IrType -> remapType(irTypeArgument) as BirTypeArgument
        is IrTypeProjectionImpl -> makeTypeProjection(remapType(irTypeArgument.type), irTypeArgument.variance)
        else -> error(irTypeArgument)
    }
}