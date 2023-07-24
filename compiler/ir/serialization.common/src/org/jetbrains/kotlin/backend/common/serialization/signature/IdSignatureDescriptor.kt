/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.renderer.DescriptorRenderer

open class IdSignatureDescriptor(override val mangler: KotlinMangler.DescriptorMangler) : IdSignatureComposer {

    protected open fun createSignatureBuilder(type: SpecialDeclarationType): DescriptorBasedSignatureBuilder = DescriptorBasedSignatureBuilder(type)

    protected open inner class DescriptorBasedSignatureBuilder(private val type: SpecialDeclarationType) :
        IdSignatureBuilder<DeclarationDescriptor, KotlinMangler.DescriptorMangler>(),
        DeclarationDescriptorVisitor<Unit, Nothing?> {

        override val mangler: KotlinMangler.DescriptorMangler
            get() = this@IdSignatureDescriptor.mangler

        override fun accept(d: DeclarationDescriptor) {
            d.accept(this, null)
        }

        private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
            error("Unexpected descriptor $descriptor")
        }

        override fun renderDeclarationForDescription(declaration: DeclarationDescriptor): String =
            DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(declaration)

        private fun collectParents(descriptor: DeclarationDescriptorNonRoot) {
            descriptor.containingDeclaration.accept(this, null)
            classFqnSegments.add(descriptor.name.asString())
        }

        private val DeclarationDescriptorWithVisibility.isPrivate: Boolean
            get() = visibility == DescriptorVisibilities.PRIVATE

        private val DeclarationDescriptorWithVisibility.isTopLevelPrivate: Boolean
            get() = isPrivate && mangler.run { !isPlatformSpecificExport() } && containingDeclaration is PackageFragmentDescriptor

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Nothing?) {
            packageFqn = descriptor.fqName
            platformSpecificPackage(descriptor)
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Nothing?) {
            packageFqn = descriptor.fqName
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Nothing?) {
            collectParents(descriptor)
            setHashIdAndDescriptionFor(descriptor, isPropertyAccessor = false)
            isTopLevelPrivate = isTopLevelPrivate or descriptor.isTopLevelPrivate

            // If this is a local function, overwrite `description` with the descriptor's rendered form.
            setDescriptionIfLocalDeclaration(descriptor)
            setExpected(descriptor.isExpect)
            platformSpecificFunction(descriptor)
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Nothing?) {
            descriptor.containingDeclaration.accept(this, null)
            createContainer()

            classFqnSegments.add(MangleConstant.TYPE_PARAMETER_MARKER_NAME)
            setHashIdAndDescription(
                descriptor.index.toLong(),
                DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor),
                isPropertyAccessor = false,
            )
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Nothing?) {
            collectParents(descriptor)
            isTopLevelPrivate = isTopLevelPrivate or descriptor.isTopLevelPrivate

            if (descriptor.kind == ClassKind.ENUM_ENTRY) {
                if (type != SpecialDeclarationType.ENUM_ENTRY) {
                    classFqnSegments.add(MangleConstant.ENUM_ENTRY_CLASS_NAME)
                }
            }

            setDescriptionIfLocalDeclaration(descriptor)
            setExpected(descriptor.isExpect)
            platformSpecificClass(descriptor)
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Nothing?) {
            collectParents(descriptor)
            isTopLevelPrivate = isTopLevelPrivate or descriptor.isTopLevelPrivate
            setExpected(descriptor.isExpect)
            platformSpecificAlias(descriptor)
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Nothing?) {
            platformSpecificModule(descriptor)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Nothing?) {
            collectParents(constructorDescriptor)
            setHashIdAndDescriptionFor(constructorDescriptor, isPropertyAccessor = false)
            platformSpecificConstructor(constructorDescriptor)
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Nothing?) =
            visitClassDescriptor(scriptDescriptor, data)

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Nothing?) {
            val actualDeclaration = if (descriptor is IrPropertyDelegateDescriptor) {
                descriptor.correspondingProperty
            } else {
                descriptor
            }
            collectParents(actualDeclaration)
            isTopLevelPrivate = isTopLevelPrivate or actualDeclaration.isTopLevelPrivate

            setHashIdAndDescriptionFor(actualDeclaration, isPropertyAccessor = false)
            setExpected(actualDeclaration.isExpect)
            platformSpecificProperty(actualDeclaration)
            if (type == SpecialDeclarationType.BACKING_FIELD) {
                if (descriptor !is IrImplementingDelegateDescriptor) {
                    createContainer()
                    classFqnSegments.add(MangleConstant.BACKING_FIELD_NAME)
                }
            }
        }

        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Nothing?) {
            descriptor.correspondingProperty.accept(this, null)
            setHashIdAndDescriptionFor(descriptor, isPropertyAccessor = true)
            classFqnSegments.add(descriptor.name.asString())
            setExpected(descriptor.isExpect)
            platformSpecificGetter(descriptor)
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Nothing?) {
            descriptor.correspondingProperty.accept(this, null)
            setHashIdAndDescriptionFor(descriptor, isPropertyAccessor = true)
            classFqnSegments.add(descriptor.name.asString())
            setExpected(descriptor.isExpect)
            platformSpecificSetter(descriptor)
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override val currentFileSignature: IdSignature.FileSignature? get() = null
    }


    override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature? {
        return if (mangler.run { descriptor.isExported(compatibleMode = false) })
            createSignatureBuilder(SpecialDeclarationType.REGULAR).buildSignature(descriptor)
        else null
    }

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature? {
        return if (mangler.run { descriptor.isExported(compatibleMode = false) })
            createSignatureBuilder(SpecialDeclarationType.ENUM_ENTRY).buildSignature(descriptor)
        else null
    }

    override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature? {
        return if (mangler.run { descriptor.isExported(compatibleMode = false) }) {
            createSignatureBuilder(SpecialDeclarationType.BACKING_FIELD).buildSignature(descriptor)
        } else null
    }

    override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature? {
        return if (mangler.run { descriptor.isExported(compatibleMode = false) })
            createSignatureBuilder(SpecialDeclarationType.ANON_INIT).buildSignature(descriptor)
        else null
    }

    override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
        body()
    }
}
