/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class SyntheticDeclarationsGenerator(context: GeneratorContext) : DeclarationDescriptorVisitor<Unit, IrDeclarationContainer?> {

    private val generator = StandaloneDeclarationGenerator(context)
    private val symbolTable = context.symbolTable

    companion object {
        private const val offset = SYNTHETIC_OFFSET
    }

    private fun <D : IrDeclaration> D.insertDeclaration(declarationContainer: IrDeclarationContainer): D {
        parent = declarationContainer
        declarationContainer.declarations.add(this)
        return this
    }

    private fun IrFunction.defaultArgumentFactory(parameter: IrValueParameter): IrExpressionBody? {
        val descriptor = parameter.descriptor as ValueParameterDescriptor
        if (!descriptor.declaresDefaultValue()) return null

        val description = "Default Argument Value stub for ${descriptor.name}|${descriptor.index}"
        return factory.createExpressionBody(IrErrorExpressionImpl(startOffset, endOffset, parameter.type, description))
    }

    private val defaultFactoryReference: IrFunction.(IrValueParameter) -> IrExpressionBody? = { defaultArgumentFactory(it) }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: IrDeclarationContainer?) {
        error("Unexpected declaration descriptor $descriptor")
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: IrDeclarationContainer?) {
        error("Unexpected declaration descriptor $descriptor")
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: IrDeclarationContainer?) {
        error("Unexpected declaration descriptor $descriptor")
    }

    private fun createFunctionStub(descriptor: FunctionDescriptor, symbol: IrSimpleFunctionSymbol): IrSimpleFunction {
        return generator.generateSimpleFunction(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol, defaultFactoryReference)
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: IrDeclarationContainer?) {
        require(data != null)
        if (descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE &&
            descriptor.kind != CallableMemberDescriptor.Kind.DELEGATION // Skip mismatching delegates, see KT-46120
        ) {
            symbolTable.descriptorExtension.declareSimpleFunctionIfNotExists(descriptor) {
                createFunctionStub(descriptor, it).insertDeclaration(data)
            }
        }
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: IrDeclarationContainer?) {
        error("Unexpected declaration descriptor $descriptor")
    }

    private fun createClassStub(descriptor: ClassDescriptor, symbol: IrClassSymbol): IrClass {
        assert(!DescriptorUtils.isEnumEntry(descriptor))
        return generator.generateClass(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol)
    }

    private fun createEnumEntruStub(descriptor: ClassDescriptor, symbol: IrEnumEntrySymbol): IrEnumEntry {
        assert(DescriptorUtils.isEnumEntry(descriptor))
        return generator.generateEnumEntry(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol)

    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: IrDeclarationContainer?) {
        require(data != null)

        if (DescriptorUtils.isEnumEntry(descriptor)) {
            symbolTable.descriptorExtension.declareEnumEntryIfNotExists(descriptor) {
                createEnumEntruStub(descriptor, it).insertDeclaration(data)
            }
        } else {
            symbolTable.descriptorExtension.declareClassIfNotExists(descriptor) {
                createClassStub(descriptor, it).insertDeclaration(data)
            }
        }
    }

    private fun declareTypeAliasStub(descriptor: TypeAliasDescriptor, symbol: IrTypeAliasSymbol): IrTypeAlias {
        return generator.generateTypeAlias(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol)
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: IrDeclarationContainer?) {
        require(data != null)

        symbolTable.descriptorExtension.declareTypeAliasIfNotExists(descriptor) {
            declareTypeAliasStub(descriptor, it).insertDeclaration(data)
        }
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: IrDeclarationContainer?) {
        error("Unreachable execution $descriptor")
    }

    private fun createConstructorStub(descriptor: ClassConstructorDescriptor, symbol: IrConstructorSymbol): IrConstructor {
        return generator.generateConstructor(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol, defaultFactoryReference)
    }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: IrDeclarationContainer?) {
        require(data != null)
        assert(constructorDescriptor is ClassConstructorDescriptor)
        symbolTable.descriptorExtension.declareConstructorIfNotExists(constructorDescriptor as ClassConstructorDescriptor) {
            createConstructorStub(constructorDescriptor, it).insertDeclaration(data)
        }
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: IrDeclarationContainer?) {
        assert(symbolTable.descriptorExtension.referenceScript(scriptDescriptor).isBound) { "Script $scriptDescriptor isn't declared" }
    }

    private fun createPropertyStub(descriptor: PropertyDescriptor, symbol: IrPropertySymbol): IrProperty {
        return generator.generateProperty(offset, offset, IrDeclarationOrigin.DEFINED, descriptor, symbol)
    }

    private fun declareAccessor(accessorDescriptor: PropertyAccessorDescriptor, property: IrProperty): IrSimpleFunction {
        // TODO: type parameters
        return symbolTable.descriptorExtension.declareSimpleFunctionIfNotExists(accessorDescriptor) {
            createFunctionStub(accessorDescriptor, it).also { acc ->
                acc.parent = property.parent
                acc.correspondingPropertySymbol = property.symbol
            }
        }
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: IrDeclarationContainer?) {
        require(data != null)
        if (descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE) {
            symbolTable.descriptorExtension.declarePropertyIfNotExists(descriptor) {
                createPropertyStub(descriptor, it).insertDeclaration(data).also { p ->
                    descriptor.getter?.let { g -> p.getter = declareAccessor(g, p) }
                    descriptor.setter?.let { s -> p.setter = declareAccessor(s, p) }
                }
            }
        }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: IrDeclarationContainer?) {
        error("Unreachable execution $descriptor")
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: IrDeclarationContainer?) {
        error("Unreachable execution $descriptor")
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: IrDeclarationContainer?) {
        error("Unreachable execution $descriptor")
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: IrDeclarationContainer?) {
        error("Unreachable execution $descriptor")
    }
}
