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

import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.indexing.FileContent
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LocalClassFinder
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LocalClassDataFinder
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.isKotlinInternalCompiledFile
import org.jetbrains.kotlin.name.FqName

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
        return when (header.kind) {
            KotlinClassHeader.Kind.PACKAGE_FACADE -> {
                val packageData = JvmProtoBufUtil.readPackageDataFrom(annotationData)
                val context = components.createContext(packageData.getNameResolver(), packageFqName)
                createPackageFacadeFileStub(packageData.getPackageProto(), packageFqName, context)
            }

            KotlinClassHeader.Kind.CLASS -> {
                val classData = JvmProtoBufUtil.readClassDataFrom(annotationData)
                val context = components.createContext(classData.getNameResolver(), packageFqName)
                createTopLevelClassStub(classId, classData.getClassProto(), context)
            }
            else -> throw IllegalStateException("Should have processed " + file.getPath() + " with ${header.kind}")
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName): ClsStubBuilderComponents {
        val localClassFinder = LocalClassFinder(file.getParent()!!, packageFqName)
        val localClassDataFinder = LocalClassDataFinder(localClassFinder, LOG)
        val annotationLoader = AnnotationLoaderForStubBuilder(localClassFinder, LoggingErrorReporter(LOG))
        return ClsStubBuilderComponents(localClassDataFinder, annotationLoader)
    }

    class object {
        val LOG = Logger.getInstance(javaClass<KotlinClsStubBuilder>())
    }
}
