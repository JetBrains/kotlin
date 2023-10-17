/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirMemberAccessExpression
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrCapturedType
import org.jetbrains.kotlin.ir.types.impl.IrDelegatedSimpleType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeProjectionImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class Ir2BirConverterBase {
    var birForest: BirForest? = null
    var copyAncestorsForOrphanedElements = false

    private val collectedBirElementsWithoutParent = mutableListOf<BirElement>()
    private val collectedIrElementsWithoutParent = mutableListOf<IrElement>()
    private var isInsideNestedElementCopy = false
    private var isInsideCopyAncestorsForOrphanedElements = false

    val currentColBirElementsWithoutParent: List<BirElement>
        get() = collectedBirElementsWithoutParent

    protected fun <Bir : BirElement, Ir : IrElement> createElementMap(expectedMaxSize: Int = 8): MutableMap<Ir, Bir> =
        IdentityHashMap<Ir, Bir>(expectedMaxSize)

    protected abstract fun <Bir : BirElement> copyElement(old: IrElement): Bir

    fun copyIrTree(irRootElements: List<IrElement>): List<BirElement> {
        return irRootElements.map { copyElement(it) }
    }

    fun copyIrTree(irRootElement: IrElement): BirElement =
        copyIrTree(listOf(irRootElement)).single()

    protected fun <Ir : IrElement, Bir : BirElement> copyNotReferencedElement(old: Ir, copy: () -> Bir): Bir {
        return doCopyElement(old, copy)
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

        return doCopyElement(old) {
            val new = copy()
            map[old] = new
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
            if (copyAncestorsForOrphanedElements || isInsideCopyAncestorsForOrphanedElements) {
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
                    remapElement<BirElement>(ir.parent)
                } else if (ir is IrFile) {
                    remapElement<BirModuleFragment>(ir.module)
                }
            }
        }
    }

    fun copyAncestorsForCollectedOrphanedElements() {
        isInsideCopyAncestorsForOrphanedElements = true
        doCopyAncestorsForCollectedOrphanedElements()
        isInsideCopyAncestorsForOrphanedElements = false
    }

    fun <Bir : BirElement> remapElement(old: IrElement): Bir = copyElement(old)

    fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(old: IrS): BirS {
        return if (old.isBound) {
            remapElement(old.owner) as BirS
        } else {
            val signature = old.signature
            @Suppress("UNCHECKED_CAST")
            when (old) {
                is IrFileSymbol -> ExternalBirSymbol.FileSymbol(signature)
                is IrExternalPackageFragmentSymbol -> ExternalBirSymbol.ExternalPackageFragmentSymbol(signature)
                is IrAnonymousInitializerSymbol -> ExternalBirSymbol.AnonymousInitializerSymbol(signature)
                is IrEnumEntrySymbol -> ExternalBirSymbol.EnumEntrySymbol(signature)
                is IrFieldSymbol -> ExternalBirSymbol.FieldSymbol(signature)
                is IrClassSymbol -> ExternalBirSymbol.ClassSymbol(signature)
                is IrScriptSymbol -> ExternalBirSymbol.ScriptSymbol(signature)
                is IrTypeParameterSymbol -> ExternalBirSymbol.TypeParameterSymbol(signature)
                is IrValueParameterSymbol -> ExternalBirSymbol.ValueParameterSymbol(signature)
                is IrVariableSymbol -> ExternalBirSymbol.VariableSymbol(signature)
                is IrConstructorSymbol -> ExternalBirSymbol.ConstructorSymbol(signature)
                is IrSimpleFunctionSymbol -> ExternalBirSymbol.SimpleFunctionSymbol(signature)
                is IrReturnableBlockSymbol -> ExternalBirSymbol.ReturnableBlockSymbol(signature)
                is IrPropertySymbol -> ExternalBirSymbol.PropertySymbol(signature)
                is IrLocalDelegatedPropertySymbol -> ExternalBirSymbol.LocalDelegatedPropertySymbol(signature)
                is IrTypeAliasSymbol -> ExternalBirSymbol.TypeAliasSymbol(signature)
                else -> error(old)
            } as BirS
        }
    }

    protected fun BirAttributeContainer.copyAttributes(old: IrAttributeContainer) {
        val owner = old.attributeOwnerId
        attributeOwnerId = if (owner === old) this
        else remapElement(owner) as BirAttributeContainer
    }

    protected fun BirElement.copyAuxData(from: IrElement) {
        this as BirElementBase
        if (from is IrMetadataSourceOwner) {
            (this as BirMetadataSourceOwner)[GlobalBirElementDynamicPropertyTokens.Metadata] = from.metadata
        }

        if (from is IrMemberWithContainerSource) {
            (this as BirMemberWithContainerSource)[GlobalBirElementDynamicPropertyTokens.ContainerSource] = from.containerSource
        }

        if (from is IrAttributeContainer) {
            (this as BirAttributeContainer)[GlobalBirElementDynamicPropertyTokens.OriginalBeforeInline] =
                from.originalBeforeInline?.let { remapElement(it) as BirAttributeContainer }
        }

        if (from is IrClass) {
            (this as BirClass)[GlobalBirElementDynamicPropertyTokens.SealedSubclasses] =
                from.sealedSubclasses.memoryOptimizedMap { remapSymbol(it) }
        }
    }

    protected fun <Ir : IrElement, Bir : BirElement> BirChildElementList<Bir>.copyElements(from: List<Ir>) {
        ensureCapacity(from.size)
        for (ir in from) {
            val bir = copyElement<Bir>(ir)
            this += bir
        }
    }

    protected fun BirMemberAccessExpression<*>.copyIrMemberAccessExpressionValueArguments(from: IrMemberAccessExpression<*>) {
        valueArguments.ensureCapacity(from.valueArgumentsCount)
        for (i in 0 until from.valueArgumentsCount) {
            val arg = from.getValueArgument(i)
            valueArguments += arg?.let { copyElement(it) as BirExpression }
        }
    }

    protected val IrMemberAccessExpression<*>.typeArguments: List<IrType?>
        get() = List(typeArgumentsCount) { getTypeArgument(it) }

    protected fun <D : DeclarationDescriptor> mapDescriptor(readDescriptor: () -> D): D {
        return readDescriptor()
    }

    fun remapType(irType: IrType): BirType = when (irType) {
        // for IrDelegatedSimpleType, this egaerly initializes a lazy IrAnnotationType
        is IrSimpleTypeImpl, is IrDelegatedSimpleType -> remapSimpleType(irType as IrSimpleType)
        is IrCapturedType -> remapCapturedType(irType)
        is IrDynamicType -> remapDynamicType(irType)
        is IrErrorType -> remapErrorType(irType)
        else -> TODO(irType.toString())
    }

    private fun remapSimpleType(irType: IrSimpleType): BirSimpleType {
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

    fun remapTypeArgument(irTypeArgument: IrTypeArgument): BirTypeArgument = when (irTypeArgument) {
        is IrStarProjection -> BirStarProjection
        is IrType -> remapType(irTypeArgument) as BirTypeArgument
        is IrTypeProjectionImpl -> makeTypeProjection(remapType(irTypeArgument.type), irTypeArgument.variance)
        else -> error(irTypeArgument)
    }

    companion object {
        fun IrElement.convertToBir(birForest: BirForest): BirElement {
            val converter = Ir2BirConverter()
            converter.birForest = birForest
            return converter.copyIrTree(listOf(this)).single()
        }
    }
}

private abstract class ExternalBirSymbol(
    override val signature: IdSignature?,
) : BirSymbol {
    class FileSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirFileSymbol
    class ExternalPackageFragmentSymbol(signature: IdSignature?) :
        ExternalBirSymbol(signature), BirExternalPackageFragmentSymbol

    class AnonymousInitializerSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirAnonymousInitializerSymbol

    class EnumEntrySymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirEnumEntrySymbol
    class FieldSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirFieldSymbol
    class ClassSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirClassSymbol
    class ScriptSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirScriptSymbol
    class TypeParameterSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirTypeParameterSymbol

    class ValueParameterSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirValueParameterSymbol

    class VariableSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirVariableSymbol
    class ConstructorSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirConstructorSymbol

    class SimpleFunctionSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirSimpleFunctionSymbol

    class ReturnableBlockSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirReturnableBlockSymbol

    class PropertySymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirPropertySymbol
    class LocalDelegatedPropertySymbol(signature: IdSignature?) :
        ExternalBirSymbol(signature), BirLocalDelegatedPropertySymbol

    class TypeAliasSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirTypeAliasSymbol
}
