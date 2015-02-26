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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.idea.decompiler.isKotlinInternalCompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DirectoryBasedClassFinder
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DirectoryBasedDataFinder
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

public class KotlinClsStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val file = content.getFile()

        if (isKotlinInternalCompiledFile(file)) {
            return null
        }

        return doBuildFileStub(file)
    }

    throws(javaClass<ClsFormatException>())
    fun doBuildFileStub(file: VirtualFile): PsiFileStub<JetFile>? {
        val kotlinBinaryClass = KotlinBinaryClassCache.getKotlinBinaryClass(file)
        val header = kotlinBinaryClass.getClassHeader()
        val classId = kotlinBinaryClass.getClassId()
        val packageFqName = classId.getPackageFqName()
        if (!header.isCompatibleAbiVersion) {
            return createIncompatibleAbiVersionFileStub()
        }

        val components = createStubBuilderComponents(file, packageFqName)
        val annotationData = header.annotationData
        if (annotationData == null) {
            LOG.error("Corrupted kotlin header for file ${file.getName()}")
            return null
        }
        return when {
            header.isCompatiblePackageFacadeKind() -> {
                val packageData = JvmProtoBufUtil.readPackageDataFrom(annotationData)
                val context = components.createContext(packageData.getNameResolver(), packageFqName)
                createPackageFacadeFileStub(packageData.getPackageProto(), packageFqName, context)
            }
            header.isCompatibleClassKind() -> {
                if (header.classKind != JvmAnnotationNames.KotlinClass.Kind.CLASS) return null
                val classData = JvmProtoBufUtil.readClassDataFrom(annotationData)
                val context = components.createContext(classData.getNameResolver(), packageFqName)
                createTopLevelClassStub(classId, classData.getClassProto(), context)
            }
            else -> throw IllegalStateException("Should have processed " + file.getPath() + " with header $header")
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName): ClsStubBuilderComponents {
        val classFinder = DirectoryBasedClassFinder(file.getParent()!!, packageFqName)
        val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)
        val annotationLoader = AnnotationLoaderForStubBuilder(classFinder, LoggingErrorReporter(LOG))
        return ClsStubBuilderComponents(classDataFinder, annotationLoader)
    }

    class object {
        val LOG = Logger.getInstance(javaClass<KotlinClsStubBuilder>())
    }
}
