/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor

class JvmIdSignatureDescriptor(private val mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

    private class JvmDescriptorBasedSignatureBuilder(mangler: KotlinMangler.DescriptorMangler) : DescriptorBasedSignatureBuilder(mangler) {
        override fun platformSpecificProperty(descriptor: PropertyDescriptor) {
            // See KT-31646
            setSpecialJavaProperty(descriptor is JavaForKotlinOverridePropertyDescriptor)
        }
    }

    override fun createSignatureBuilder(): DescriptorBasedSignatureBuilder = JvmDescriptorBasedSignatureBuilder(mangler)
}