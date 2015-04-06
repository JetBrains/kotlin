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
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.idea.decompiler.AnnotationLoaderForKotlinJavaScriptStubBuilder
import org.jetbrains.kotlin.idea.decompiler.isKotlinJavaScriptInternalCompiledFile
import org.jetbrains.kotlin.idea.decompiler.navigation.JsMetaFileUtils
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DirectoryBasedKotlinJavaScriptDataFinder
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DirectoryBasedKotlinJavaScriptMetaFileFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializedResourcePaths
import org.jetbrains.kotlin.serialization.js.toClassData
import org.jetbrains.kotlin.serialization.js.toPackageData
import java.io.ByteArrayInputStream

public open class KotlinJavaScriptStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val file = content.getFile()

        if (isKotlinJavaScriptInternalCompiledFile(file)) return null

        return doBuildFileStub(file)
    }

    fun doBuildFileStub(file: VirtualFile): PsiFileStub<JetFile>? {
        val packageFqName = JsMetaFileUtils.getPackageFqName(file)

        val content = file.contentsToByteArray(false)
        val isPackageHeader = JsMetaFileUtils.isPackageHeader(file)

        val moduleDirectory = JsMetaFileUtils.getModuleDirectory(file)
        val stringsFileName = KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(packageFqName)
        val stringsFile = moduleDirectory.findFileByRelativePath(stringsFileName)
        assert(stringsFile != null, "strings file not found: $stringsFileName")

        val nameResolver = NameResolver.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))
        val components = createStubBuilderComponents(file, packageFqName, nameResolver)

        if (isPackageHeader) {
            val packageData = content.toPackageData(nameResolver)
            val context = components.createContext(packageData.getNameResolver(), packageFqName)
            return createPackageFacadeFileStub(packageData.getPackageProto(), packageFqName, context)
        }
        else {
            val classData =  content.toClassData(nameResolver)
            val context = components.createContext(classData.getNameResolver(), packageFqName)
            val classId = JsMetaFileUtils.getClassId(file)
            return createTopLevelClassStub(classId, classData.getClassProto(), context)
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName, nameResolver: NameResolver): ClsStubBuilderComponents {
        val metaFileFinder = DirectoryBasedKotlinJavaScriptMetaFileFinder(file.getParent()!!, packageFqName, nameResolver)
        val classDataFinder = DirectoryBasedKotlinJavaScriptDataFinder(metaFileFinder, LOG)
        val annotationLoader = AnnotationLoaderForKotlinJavaScriptStubBuilder()
        return ClsStubBuilderComponents(classDataFinder, annotationLoader)
    }

    companion object {
        val LOG = Logger.getInstance(javaClass<KotlinJavaScriptStubBuilder>())
    }
}

