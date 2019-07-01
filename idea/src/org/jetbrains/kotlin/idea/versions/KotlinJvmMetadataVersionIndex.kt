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

import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

object KotlinJvmMetadataVersionIndex : KotlinMetadataVersionIndexBase<KotlinJvmMetadataVersionIndex, JvmMetadataVersion>(
    KotlinJvmMetadataVersionIndex::class.java
) {
    override fun createBinaryVersion(versionArray: IntArray, extraBoolean: Boolean?): JvmMetadataVersion =
        JvmMetadataVersion(versionArray, isStrictSemantics = extraBoolean!!)

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { file -> file.fileType == StdFileTypes.CLASS }

    override fun getVersion() = VERSION

    override fun isExtraBooleanNeeded(): Boolean = true

    override fun getExtraBoolean(version: JvmMetadataVersion): Boolean = version.isStrictSemantics

    private const val VERSION = 5

    private val kindsToIndex: Set<KotlinClassHeader.Kind> by lazy {
        setOf(
            KotlinClassHeader.Kind.CLASS,
            KotlinClassHeader.Kind.FILE_FACADE,
            KotlinClassHeader.Kind.MULTIFILE_CLASS
        )
    }

    private val INDEXER: DataIndexer<JvmMetadataVersion, Void, FileContent> by lazy {
        DataIndexer<JvmMetadataVersion, Void, FileContent> { inputData: FileContent ->
            var versionArray: IntArray? = null
            var isStrictSemantics = false
            var annotationPresent = false
            var kind: KotlinClassHeader.Kind? = null

            tryBlock(inputData) {
                val classReader = ClassReader(inputData.content)
                classReader.accept(object : ClassVisitor(Opcodes.API_VERSION) {
                    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                        if (desc != METADATA_DESC) return null

                        annotationPresent = true
                        return object : AnnotationVisitor(Opcodes.API_VERSION) {
                            override fun visit(name: String, value: Any) {
                                when (name) {
                                    METADATA_VERSION_FIELD_NAME -> if (value is IntArray) {
                                        versionArray = value
                                    }
                                    KIND_FIELD_NAME -> if (value is Int) {
                                        kind = KotlinClassHeader.Kind.getById(value)
                                    }
                                    METADATA_EXTRA_INT_FIELD_NAME -> if (value is Int) {
                                        isStrictSemantics = (value and METADATA_STRICT_VERSION_SEMANTICS_FLAG) != 0
                                    }
                                }
                            }
                        }
                    }
                }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }

            var version =
                if (versionArray != null) createBinaryVersion(versionArray!!, isStrictSemantics) else null

            if (kind !in kindsToIndex) {
                // Do not index metadata version for synthetic classes
                version = null
            } else if (annotationPresent && version == null) {
                // No version at all because the class is too old, or version is set to something weird
                version = JvmMetadataVersion.INVALID_VERSION
            }

            if (version != null) mapOf(version to null) else emptyMap()
        }
    }
}
