/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType

internal inline fun <reified T: DeclarationDescriptor> ClassDescriptor.findDeclarationByName(name: String): T? =
        unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<T>()
                .firstOrNull { it.name.identifier == name }

/**
 * Provides a set of functions and properties that helps
 * to translate descriptor declarations to corresponding IR.
 */
internal interface DescriptorToIrTranslationMixin {

    val symbolTable: SymbolTable

    val irBuiltIns: IrBuiltIns

    val typeTranslator: TypeTranslator

    val stubGenerator: DeclarationStubGenerator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    /**
     * Declares [IrClass] instance from [descriptor] and populates it with
     * supertypes, <this> parameter declaration and fake overrides.
     * Additional elements are passed via [builder] callback.
     */
    fun createClass(descriptor: ClassDescriptor, builder: (IrClass) -> Unit): IrClass =
            symbolTable.declareClass(
                    startOffset = SYNTHETIC_OFFSET,
                    endOffset = SYNTHETIC_OFFSET,
                    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    descriptor = descriptor
            ).also { irClass ->
                symbolTable.withScope(descriptor) {
                    descriptor.typeConstructor.supertypes.mapTo(irClass.superTypes) {
                        it.toIrType()
                    }
                    irClass.createParameterDeclarations()
                    builder(irClass)
                    createFakeOverrides(descriptor).forEach(irClass::addMember)
                }
            }

    private fun createFakeOverrides(classDescriptor: ClassDescriptor): List<IrDeclaration> {
        val fakeOverrides = classDescriptor.unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
        return fakeOverrides.map {
            when (it) {
                is PropertyDescriptor -> createProperty(it)
                is FunctionDescriptor -> createFunction(it, IrDeclarationOrigin.FAKE_OVERRIDE)
                else -> error("Unexpected fake override descriptor: $it")
            } as IrDeclaration // Assistance for type inference.
        }
    }

    fun createConstructor(constructorDescriptor: ClassConstructorDescriptor): IrConstructor =
            stubGenerator.generateMemberStub(constructorDescriptor) as IrConstructor

    fun createProperty(propertyDescriptor: PropertyDescriptor): IrProperty =
            stubGenerator.generateMemberStub(propertyDescriptor) as IrProperty

    fun createFunction(
            functionDescriptor: FunctionDescriptor,
            origin: IrDeclarationOrigin? = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    ): IrSimpleFunction =
            stubGenerator.generateFunctionStub(functionDescriptor, createPropertyIfNeeded = false).also {
                if (origin != null) it.origin = origin
            }
}

internal fun IrBuilder.irInstanceInitializer(classSymbol: IrClassSymbol): IrExpression =
        IrInstanceInitializerCallImpl(
                startOffset, endOffset,
                classSymbol,
                context.irBuiltIns.unitType
        )

internal fun ClassDescriptor.implementsCEnum(interopBuiltIns: InteropBuiltIns): Boolean =
        interopBuiltIns.cEnum in this.getSuperInterfaces()

/**
 * All enums that come from interop library implement CEnum interface.
 * This function checks that given symbol located in subtree of
 * CEnum inheritor.
 */
internal fun IrSymbol.findCEnumDescriptor(interopBuiltIns: InteropBuiltIns): ClassDescriptor? =
        descriptor.parentsWithSelf
                .filterIsInstance<ClassDescriptor>()
                .firstOrNull { it.implementsCEnum(interopBuiltIns) }