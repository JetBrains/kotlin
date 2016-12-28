package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.util.nTabs
import org.jetbrains.kotlin.renderer.*

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

public class DeepPrintVisitor(worker: DeclarationDescriptorVisitor<Boolean, Int>): DeepVisitor< Int>(worker) {

    override public fun visitChildren(descriptor: DeclarationDescriptor?, data: Int): Boolean {
        return super.visitChildren(descriptor, data+1)
    }

    override public fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: Int): Boolean {
        return super.visitChildren(descriptors, data+1)
    }
}

public class PrintVisitor: DeclarationDescriptorVisitor<Boolean, Int> {

    fun printDescriptor(descriptor: DeclarationDescriptor, amount: Int): Boolean {
        println("${nTabs(amount)} ${descriptor.toString()}")
        return true;
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)
}


