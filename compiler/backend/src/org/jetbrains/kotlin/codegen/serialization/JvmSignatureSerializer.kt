/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.serialization

import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.serialization.StringTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.commons.Method

abstract class JvmSignatureSerializer<F, P>(val stringTable: StringTable) {
    fun methodSignature(descriptor: F?, descriptorName: Name?, method: Method): JvmProtoBuf.JvmMethodSignature? {
        val builder = JvmProtoBuf.JvmMethodSignature.newBuilder()
        if (descriptor == null || descriptorName?.asString() != method.name) {
            builder.name = stringTable.getStringIndex(method.name)
        }
        if (descriptor == null || requiresFunctionSignature(descriptor, method.descriptor)) {
            builder.desc = stringTable.getStringIndex(method.descriptor)
        }
        return if (builder.hasName() || builder.hasDesc()) builder.build() else null
    }

    protected abstract fun requiresFunctionSignature(descriptor: F, desc: String): Boolean

    abstract fun requiresPropertySignature(descriptor: P, desc: String): Boolean

    fun propertySignature(
        descriptorName: Name,
        fieldName: String?,
        fieldDesc: String?,
        syntheticMethod: JvmProtoBuf.JvmMethodSignature?,
        delegateMethod: JvmProtoBuf.JvmMethodSignature?,
        getter: JvmProtoBuf.JvmMethodSignature?,
        setter: JvmProtoBuf.JvmMethodSignature?,
        requiresFieldSignature: Boolean
    ): JvmProtoBuf.JvmPropertySignature? {
        val signature = JvmProtoBuf.JvmPropertySignature.newBuilder()

        if (fieldDesc != null) {
            assert(fieldName != null) { "Field name shouldn't be null when there's a field type: $fieldDesc" }
            signature.field = fieldSignature(descriptorName, fieldName!!, fieldDesc, requiresFieldSignature, stringTable)
        }

        if (syntheticMethod != null) {
            signature.syntheticMethod = syntheticMethod
        }

        if (delegateMethod != null) {
            signature.delegateMethod = delegateMethod
        }

        if (getter != null) {
            signature.getter = getter
        }
        if (setter != null) {
            signature.setter = setter
        }

        return signature.build().takeIf { it.serializedSize > 0 }
    }

    private fun fieldSignature(
        propertyName: Name,
        fieldName: String,
        fieldDesc: String,
        requiresFieldSignature: Boolean,
        stringTable: StringTable
    ): JvmProtoBuf.JvmFieldSignature {
        val builder = JvmProtoBuf.JvmFieldSignature.newBuilder()
        if (propertyName.asString() != fieldName) {
            builder.name = stringTable.getStringIndex(fieldName)
        }
        if (requiresFieldSignature) {
            builder.desc = stringTable.getStringIndex(fieldDesc)
        }
        return builder.build()
    }
}