/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.util.TextifierSupport

const val LOADABLE_DESCRIPTORS_ATTRIBUTE_NAME = "LoadableDescriptors"

/**
 * The `LoadableDescriptors` class-file attribute (JEP 401), listing field descriptors of value-class field types so the JVM may
 * eagerly load them and flatten their storage. Layout: `u2 number_of_descriptors; u2 descriptors[number_of_descriptors]`, where
 * each entry is a constant-pool index of a `CONSTANT_Utf8` holding a field descriptor.
 */
class LoadableDescriptorsAttribute(val descriptors: List<String>) :
    Attribute(LOADABLE_DESCRIPTORS_ATTRIBUTE_NAME), TextifierSupport {
    override fun write(
        classWriter: ClassWriter, code: ByteArray?, codeLength: Int, maxStack: Int, maxLocals: Int
    ): ByteVector = ByteVector().apply {
        putShort(descriptors.size)
        for (descriptor in descriptors) {
            putShort(classWriter.newUTF8(descriptor))
        }
    }

    override fun read(
        classReader: ClassReader, offset: Int, length: Int, charBuffer: CharArray?, codeAttributeOffset: Int, labels: Array<Label>?,
    ): Attribute {
        val count = classReader.readUnsignedShort(offset)
        val descriptors = ArrayList<String>(count)
        var descriptorOffset = offset + 2
        repeat(count) {
            descriptors.add(classReader.readUTF8(descriptorOffset, charBuffer))
            descriptorOffset += 2
        }
        return LoadableDescriptorsAttribute(descriptors)
    }

    override fun textify(stringBuilder: StringBuilder, labelNames: MutableMap<Label, String>?) {
        stringBuilder.append(descriptors.joinToString(prefix = " : ", postfix = "\n"))
    }
}

/**
 * Attribute prototypes to hand to `ClassReader.accept` so a [LoadableDescriptorsAttribute] present on an already-compiled class is
 * parsed structurally — and therefore re-emitted with valid constant-pool indices — instead of being copied as raw, dangling bytes.
 * The prototype is only read from (its `type` is matched and [LoadableDescriptorsAttribute.read] returns a fresh instance), so the
 * shared array is safe to reuse across concurrent class rewrites.
 */
val LOADABLE_DESCRIPTORS_ATTRIBUTE_PROTOTYPES: Array<Attribute> = arrayOf(LoadableDescriptorsAttribute(emptyList()))
