/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.org.objectweb.asm.*

internal fun transformMetadataInClassFile(bytes: ByteArray, transform: (fieldName: String, value: Any?) -> Any?): ByteArray {
    val writer = ClassWriter(0)
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION, writer) {
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            val superVisitor = super.visitAnnotation(desc, visible)
            if (desc == JvmAnnotationNames.METADATA_DESC) {
                return object : AnnotationVisitor(Opcodes.API_VERSION, superVisitor) {
                    override fun visit(name: String, value: Any) {
                        super.visit(name, transform(name, value) ?: value)
                    }

                    override fun visitArray(name: String): AnnotationVisitor {
                        val entries = arrayListOf<String>()
                        val arrayVisitor = { super.visitArray(name) }
                        return object : AnnotationVisitor(Opcodes.API_VERSION) {
                            override fun visit(name: String?, value: Any) {
                                entries.add(value as String)
                            }

                            override fun visitEnd() {
                                @Suppress("UNCHECKED_CAST")
                                val result = transform(name, entries.toTypedArray()) as Array<String>? ?: entries.toTypedArray()
                                if (result.isEmpty()) return
                                with(arrayVisitor()) {
                                    for (value in result) {
                                        visit(null, value)
                                    }
                                    visitEnd()
                                }
                            }
                        }
                    }
                }
            }
            return superVisitor
        }
    }, 0)
    return writer.toByteArray()
}
