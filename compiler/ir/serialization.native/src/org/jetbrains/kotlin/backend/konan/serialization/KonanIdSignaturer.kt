/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler

class KonanIdSignaturer(mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

    override fun createSignatureBuilder(type: SpecialDeclarationType): DescriptorBasedSignatureBuilder =
            KonanDescriptorBasedSignatureBuilder(type)

    private inner class KonanDescriptorBasedSignatureBuilder(
            type: SpecialDeclarationType
    ) : DescriptorBasedSignatureBuilder(type) {

        /**
         * We need a way to distinguish interop declarations from usual ones
         * to be able to link against them. We do it by marking them with
         * [IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY] flag.
         */
        private fun markInteropDeclaration(descriptor: DeclarationDescriptor) {
            if (descriptor.isFromInteropLibrary()) {
                mask = mask or IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(true)
            }
        }

        override fun platformSpecificAlias(descriptor: TypeAliasDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificClass(descriptor: ClassDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificConstructor(descriptor: ConstructorDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificFunction(descriptor: FunctionDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificGetter(descriptor: PropertyGetterDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificPackage(descriptor: PackageFragmentDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificProperty(descriptor: PropertyDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificSetter(descriptor: PropertySetterDescriptor) {
            markInteropDeclaration(descriptor)
        }
    }
}
