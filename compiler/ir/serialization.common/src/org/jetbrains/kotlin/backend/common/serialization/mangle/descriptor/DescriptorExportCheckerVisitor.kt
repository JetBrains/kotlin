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
import org.jetbrains.kotlin.descriptors.annotations.Annotations

abstract class DescriptorExportCheckerVisitor : DeclarationDescriptorVisitor<Boolean, SpecialDeclarationType>,
    KotlinExportChecker<DeclarationDescriptor> {

    override fun check(declaration: DeclarationDescriptor, type: SpecialDeclarationType): Boolean {
        return declaration.accept(this, type)
    }

    private fun DescriptorVisibility.isPubliclyVisible(): Boolean = isPublicAPI || this === DescriptorVisibilities.INTERNAL

    private fun DeclarationDescriptorNonRoot.isExported(annotations: Annotations, visibility: DescriptorVisibility?): Boolean {
        if (visibility == DescriptorVisibilities.LOCAL) return false
        return if (containingDeclaration is PackageFragmentDescriptor) {
            val speciallyExported = annotations.hasAnnotation(publishedApiAnnotation) || isPlatformSpecificExported()
            return speciallyExported || visibility?.isPubliclyVisible() ?: error("VISIBILITY == null: $this")

        } else containingDeclaration.accept(this@DescriptorExportCheckerVisitor, SpecialDeclarationType.REGULAR)
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: SpecialDeclarationType) = true

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: SpecialDeclarationType) = true

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: SpecialDeclarationType) = false

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: SpecialDeclarationType): Boolean {
        if (descriptor.name.isAnonymous) return false
        return descriptor.run { isExported(annotations, visibility) }
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: SpecialDeclarationType): Boolean {
        return descriptor.containingDeclaration.accept(this, data)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: SpecialDeclarationType): Boolean {
        if (data == SpecialDeclarationType.ANON_INIT) return false
        if (descriptor.name.isAnonymous) return false
        return descriptor.run { isExported(annotations, visibility) }
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: SpecialDeclarationType): Boolean {
        return descriptor.run { isExported(annotations, visibility) }
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: SpecialDeclarationType): Boolean {
        return false
    }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: SpecialDeclarationType): Boolean {
        return constructorDescriptor.constructedClass.run { isExported(annotations, visibility) }
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: SpecialDeclarationType): Boolean = false

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: SpecialDeclarationType): Boolean {
        return descriptor.run { isExported(annotations, visibility) }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: SpecialDeclarationType): Boolean {
        return false
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: SpecialDeclarationType): Boolean {
        return descriptor.correspondingProperty.run { isExported(annotations, visibility) }
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: SpecialDeclarationType): Boolean {
        return descriptor.correspondingProperty.run { isExported(annotations, visibility) }
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: SpecialDeclarationType) = false

}