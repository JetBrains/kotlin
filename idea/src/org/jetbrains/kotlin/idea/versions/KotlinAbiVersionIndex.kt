/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.versions

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.ExternalIntegerKeyDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.load.java.AbiVersionUtil
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

import org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*

/**
 * Important! This is not a stub-based index. And it has its own version
 */
public class KotlinAbiVersionIndex private constructor() : ScalarIndexExtension<Int>() {

    override fun getName(): ID<Int, Void> {
        return NAME
    }

    override fun getIndexer(): DataIndexer<Int, Void, FileContent> {
        return INDEXER
    }

    override fun getKeyDescriptor(): KeyDescriptor<Int> {
        return KEY_DESCRIPTOR
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return INPUT_FILTER
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getVersion(): Int {
        return VERSION
    }

    companion object {
        private val LOG = Logger.getInstance(javaClass<KotlinAbiVersionIndex>())

        public val INSTANCE: KotlinAbiVersionIndex = KotlinAbiVersionIndex()

        private val VERSION = 1

        private val NAME = ID.create<Int, Void>(javaClass<KotlinAbiVersionIndex>().getCanonicalName())
        private val KEY_DESCRIPTOR = ExternalIntegerKeyDescriptor()

        private val INPUT_FILTER = object : FileBasedIndex.InputFilter {
            override fun acceptInput(file: VirtualFile): Boolean {
                return file.getFileType() == StdFileTypes.CLASS
            }
        }

        private val INDEXER = object : DataIndexer<Int, Void, FileContent> {
            SuppressWarnings("deprecation")
            private val kotlinAnnotationsDesc = ImmutableSet.Builder<String>().add(asmDescByFqNameWithoutInnerClasses(OLD_JET_CLASS_ANNOTATION)).add(asmDescByFqNameWithoutInnerClasses(OLD_JET_PACKAGE_CLASS_ANNOTATION)).add(asmDescByFqNameWithoutInnerClasses(OLD_KOTLIN_CLASS)).add(asmDescByFqNameWithoutInnerClasses(OLD_KOTLIN_PACKAGE)).add(asmDescByFqNameWithoutInnerClasses(KOTLIN_CLASS)).add(asmDescByFqNameWithoutInnerClasses(KOTLIN_PACKAGE)).build()

            override fun map(inputData: FileContent): Map<Int, Void> {
                val result = Maps.newHashMap<Int, Void>()
                val annotationPresent = Ref(false)

                try {
                    val classReader = ClassReader(inputData.getContent())
                    classReader.accept(object : ClassVisitor(Opcodes.ASM5) {
                        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                            if (!kotlinAnnotationsDesc.contains(desc)) {
                                return null
                            }
                            annotationPresent.set(true)
                            return object : AnnotationVisitor(Opcodes.ASM5) {
                                override fun visit(name: String, value: Any) {
                                    if (ABI_VERSION_FIELD_NAME == name) {
                                        if (value is Int) {
                                            result.put(value, null)
                                        }
                                        else {
                                            // Version is set to something weird
                                            result.put(AbiVersionUtil.INVALID_VERSION, null)
                                        }
                                    }
                                }
                            }
                        }
                    }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                }
                catch (e: Throwable) {
                    LOG.warn("Could not index ABI version for file " + inputData.getFile() + ": " + e.getMessage())
                }


                if (annotationPresent.get() && result.isEmpty()) {
                    // No version at all: the class is too old
                    result.put(AbiVersionUtil.INVALID_VERSION, null)
                }

                return result
            }
        }
    }
}
