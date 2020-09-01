/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop.cenum

import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.ir.interop.DescriptorToIrTranslationMixin
import org.jetbrains.kotlin.backend.konan.ir.interop.findDeclarationByName
import org.jetbrains.kotlin.backend.konan.ir.interop.irInstanceInitializer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.DeclarationGenerator
import org.jetbrains.kotlin.psi2ir.generators.EnumClassMembersGenerator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

private fun extractConstantValue(descriptor: DeclarationDescriptor, type: String): ConstantValue<*>? =
        descriptor.annotations
                .findAnnotation(cEnumEntryValueAnnotationName.child(Name.identifier(type)))
                ?.allValueArguments
                ?.getValue(Name.identifier("value"))

private val cEnumEntryValueAnnotationName = FqName("kotlinx.cinterop.internal.ConstantValue")

private val cEnumEntryValueTypes = setOf(
        "Byte", "Short", "Int", "Long",
        "UByte", "UShort", "UInt", "ULong"
)

internal class CEnumClassGenerator(
        val context: GeneratorContext,
        private val cEnumCompanionGenerator: CEnumCompanionGenerator,
        private val cEnumVarClassGenerator: CEnumVarClassGenerator
) : DescriptorToIrTranslationMixin {

    override val irBuiltIns: IrBuiltIns = context.irBuiltIns
    override val symbolTable: SymbolTable = context.symbolTable
    override val typeTranslator: TypeTranslator = context.typeTranslator

    private val enumClassMembersGenerator = EnumClassMembersGenerator(DeclarationGenerator(context))

    /**
     * Searches for an IR class for [classDescriptor] in symbol table.
     * Generates one if absent.
     */
    fun findOrGenerateCEnum(classDescriptor: ClassDescriptor, parent: IrDeclarationContainer): IrClass {
        val irClassSymbol = symbolTable.referenceClass(classDescriptor)
        return if (!irClassSymbol.isBound) {
            provideIrClassForCEnum(classDescriptor).also {
                it.patchDeclarationParents(parent)
                parent.declarations += it
            }
        } else {
            irClassSymbol.owner
        }
    }

    /**
     * The main function that for given [descriptor] of the enum generates the whole
     * IR tree including entries, CEnumVar class, and companion objects.
     */
    private fun provideIrClassForCEnum(descriptor: ClassDescriptor): IrClass =
            createClass(descriptor) { enumIrClass ->
                enumIrClass.addMember(createEnumPrimaryConstructor(descriptor))
                enumIrClass.addMember(createValueProperty(enumIrClass))
                descriptor.enumEntries.mapTo(enumIrClass.declarations) { entryDescriptor ->
                    createEnumEntry(descriptor, entryDescriptor)
                }
                enumClassMembersGenerator.generateSpecialMembers(enumIrClass)
                enumIrClass.addChild(cEnumCompanionGenerator.generate(enumIrClass))
                enumIrClass.addChild(cEnumVarClassGenerator.generate(enumIrClass))
            }

    /**
     * Creates `value` property that stores integral value of the enum.
     */
    private fun createValueProperty(irClass: IrClass): IrProperty {
        val propertyDescriptor = irClass.descriptor
                .findDeclarationByName<PropertyDescriptor>("value")
                ?: error("No `value` property in ${irClass.name}")
        val irProperty = createProperty(propertyDescriptor)
        symbolTable.withScope(propertyDescriptor) {
            irProperty.backingField = symbolTable.declareField(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                    propertyDescriptor, propertyDescriptor.type.toIrType(), DescriptorVisibilities.PRIVATE
            ).also {
                it.initializer = irBuilder(irBuiltIns, it.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).run {
                    irExprBody(irGet(irClass.primaryConstructor!!.valueParameters[0]))
                }
            }
        }
        val getter = irProperty.getter!!
        getter.correspondingPropertySymbol = irProperty.symbol
        getter.body = irBuilder(irBuiltIns, getter.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +irReturn(
                    irGetField(
                            irGet(getter.dispatchReceiverParameter!!),
                            irProperty.backingField!!
                    )
            )
        }
        return irProperty
    }

    private fun createEnumEntry(enumDescriptor: ClassDescriptor, entryDescriptor: ClassDescriptor): IrEnumEntry {
        return symbolTable.declareEnumEntry(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, entryDescriptor
        ).also { enumEntry ->
            enumEntry.initializerExpression = IrExpressionBodyImpl(IrEnumConstructorCallImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    type = irBuiltIns.unitType,
                    symbol = symbolTable.referenceConstructor(enumDescriptor.unsubstitutedPrimaryConstructor!!),
                    typeArgumentsCount = 0 // enums can't be generic
            ).also {
                it.putValueArgument(0, extractEnumEntryValue(entryDescriptor))
            })
        }
    }

    /**
     * Every enum entry that came from metadata-based interop library is annotated with
     * [kotlinx.cinterop.internal.ConstantValue] annotation that holds internal constant value of the
     * corresponding entry.
     *
     * This function extracts value from the annotation.
     */
    private fun extractEnumEntryValue(entryDescriptor: ClassDescriptor): IrExpression =
            cEnumEntryValueTypes.firstNotNullResult { extractConstantValue(entryDescriptor, it) } ?.let {
                context.constantValueGenerator.generateConstantValueAsExpression(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, it)
            } ?: error("Enum entry $entryDescriptor has no appropriate @$cEnumEntryValueAnnotationName annotation!")

    private fun createEnumPrimaryConstructor(descriptor: ClassDescriptor): IrConstructor {
        val irConstructor = createConstructor(descriptor.unsubstitutedPrimaryConstructor!!)
        val enumConstructor = context.builtIns.enum.constructors.single()
        irConstructor.body = irBuilder(irBuiltIns, irConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            +IrEnumConstructorCallImpl(
                    startOffset, endOffset,
                    context.irBuiltIns.unitType,
                    symbolTable.referenceConstructor(enumConstructor),
                    typeArgumentsCount = 1 // kotlin.Enum<T> has a single type parameter.
            ).apply {
                putTypeArgument(0, descriptor.defaultType.toIrType())
            }
            +irInstanceInitializer(symbolTable.referenceClass(descriptor))
        }
        return irConstructor
    }
}
