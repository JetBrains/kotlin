/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.isAnonymous
import org.jetbrains.kotlin.backend.common.serialization.mangle.publishedApiAnnotation
import org.jetbrains.kotlin.descriptors.*

abstract class DescriptorExportCheckerVisitor : DeclarationDescriptorVisitor<Boolean, SpecialDeclarationType>,
    KotlinExportChecker<DeclarationDescriptor> {

    override fun check(declaration: DeclarationDescriptor, type: SpecialDeclarationType): Boolean =
        declaration.accept(this, type)

    private fun <D> D.isExported(): Boolean where D : DeclarationDescriptorNonRoot, D : DeclarationDescriptorWithVisibility {
        if (getContainingDeclaration() is PackageFragmentDescriptor) {
            val visibility = visibility
            if (visibility.isPublicAPI || visibility === DescriptorVisibilities.INTERNAL) return true
            if (visibility === DescriptorVisibilities.LOCAL) return false
            return annotations.hasAnnotation(publishedApiAnnotation) || isPlatformSpecificExported()
        }

        return visibility !== DescriptorVisibilities.LOCAL &&
                getContainingDeclaration().accept(this@DescriptorExportCheckerVisitor, SpecialDeclarationType.REGULAR)
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: SpecialDeclarationType) = true

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: SpecialDeclarationType) = true

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: SpecialDeclarationType) = false

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: SpecialDeclarationType): Boolean =
        !descriptor.name.isAnonymous && descriptor.isExported()

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: SpecialDeclarationType): Boolean =
        descriptor.containingDeclaration.accept(this, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: SpecialDeclarationType): Boolean {
        if (data == SpecialDeclarationType.ANON_INIT) return false
        if (descriptor.name.isAnonymous) return false
        return descriptor.isExported()
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: SpecialDeclarationType): Boolean =
        descriptor.isExported()

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: SpecialDeclarationType): Boolean = false

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: SpecialDeclarationType): Boolean =
        constructorDescriptor.constructedClass.isExported()

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: SpecialDeclarationType): Boolean = false

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: SpecialDeclarationType): Boolean =
        descriptor.isExported()

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: SpecialDeclarationType): Boolean = false

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: SpecialDeclarationType): Boolean =
        descriptor.correspondingProperty.isExported()

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: SpecialDeclarationType): Boolean =
        descriptor.correspondingProperty.isExported()

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: SpecialDeclarationType) = false
}
