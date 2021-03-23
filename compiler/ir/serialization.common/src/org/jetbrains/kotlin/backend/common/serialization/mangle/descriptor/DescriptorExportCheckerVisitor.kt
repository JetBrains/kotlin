/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

//abstract class DescriptorExportCheckerVisitor : DeclarationDescriptorVisitor<Boolean, SpecialDeclarationType>,
//    KotlinExportChecker<DeclarationDescriptor> {
//
//    override fun check(declaration: DeclarationDescriptor, type: SpecialDeclarationType): Boolean {
//        return declaration.accept(this, type)
//    }
//
//    private fun DescriptorVisibility.isPubliclyVisible(): Boolean = isPublicAPI || this === DescriptorVisibilities.INTERNAL
//
//    private fun DeclarationDescriptorNonRoot.isExported(annotations: Annotations, visibility: DescriptorVisibility?): Boolean {
//        val speciallyExported = annotations.hasAnnotation(publishedApiAnnotation) || isPlatformSpecificExported()
//        val selfExported = speciallyExported || visibility == null || visibility.isPubliclyVisible()
//
//        return selfExported && containingDeclaration.accept(this@DescriptorExportCheckerVisitor, SpecialDeclarationType.REGULAR) ?: false
//    }
//
//    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: SpecialDeclarationType) = true
//
//    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: SpecialDeclarationType) = true
//
//    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: SpecialDeclarationType) = false
//
//    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: SpecialDeclarationType): Boolean {
//        return descriptor.run { isExported(annotations, visibility) }
//    }
//
//    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: SpecialDeclarationType): Boolean = false
//
//    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: SpecialDeclarationType): Boolean {
//        if (data == SpecialDeclarationType.ANON_INIT) return false
//        if (descriptor.name == SpecialNames.NO_NAME_PROVIDED) return false
//        if (descriptor.kind == ClassKind.ENUM_ENTRY && data == SpecialDeclarationType.REGULAR) return false
//        return descriptor.run { isExported(annotations, visibility) }
//    }
//
//    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: SpecialDeclarationType) =
//        if (descriptor.containingDeclaration is PackageFragmentDescriptor) true
//        else descriptor.run { isExported(annotations, visibility) }
//
//    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: SpecialDeclarationType): Boolean {
//        return false
//    }
//
//    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: SpecialDeclarationType): Boolean {
//        val klass = constructorDescriptor.constructedClass
//        return if (klass.kind.isSingleton)
//            klass.accept(this, SpecialDeclarationType.REGULAR)
//        else constructorDescriptor.run { isExported(annotations, visibility) }
//    }
//
//    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: SpecialDeclarationType): Boolean = false
//
//    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: SpecialDeclarationType): Boolean {
//        val visibility = if (data == SpecialDeclarationType.BACKING_FIELD) {
//            return false
//        } else descriptor.visibility
//
//        return descriptor.run { isExported(annotations, visibility) }
//    }
//
//    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: SpecialDeclarationType): Boolean {
//        return false
//    }
//
//    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: SpecialDeclarationType): Boolean {
//        return descriptor.run { isExported(correspondingProperty.annotations, visibility) }
//    }
//
//    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: SpecialDeclarationType): Boolean {
//        return descriptor.run { isExported(correspondingProperty.annotations, visibility) }
//    }
//
//    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: SpecialDeclarationType) = false
//
//}
