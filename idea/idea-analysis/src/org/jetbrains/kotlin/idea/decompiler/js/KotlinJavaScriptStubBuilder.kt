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

package org.jetbrains.kotlin.idea.decompiler.js

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.idea.decompiler.common.AnnotationLoaderForStubBuilderImpl
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.ClsStubBuilderComponents
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.createPackageFacadeStub
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.createTopLevelClassStub
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptClassDataFinder
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializedResourcePaths
import java.io.ByteArrayInputStream

class KotlinJavaScriptStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val file = content.file

        if (JsMetaFileUtils.isKotlinJavaScriptInternalCompiledFile(file)) return null

        return doBuildFileStub(file)
    }

    fun doBuildFileStub(file: VirtualFile): PsiFileStub<KtFile>? {
        val packageFqName = JsMetaFileUtils.getPackageFqName(file)

        val content = file.contentsToByteArray(false)
        val isPackageHeader = JsMetaFileUtils.isPackageHeader(file)

        val moduleDirectory = JsMetaFileUtils.getModuleDirectory(file)
        val stringsFileName = KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(packageFqName)
        val stringsFile = moduleDirectory.findFileByRelativePath(stringsFileName)
        assert(stringsFile != null) { "strings file not found: $stringsFileName" }

        val nameResolver = NameResolverImpl.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))
        val components = createStubBuilderComponents(file, nameResolver)

        if (isPackageHeader) {
            val packageProto = ProtoBuf.Package.parseFrom(content, KotlinJavascriptSerializedResourcePaths.extensionRegistry)
            val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
            return createPackageFacadeStub(packageProto, packageFqName, context)
        }
        else {
            val classProto = ProtoBuf.Class.parseFrom(content, KotlinJavascriptSerializedResourcePaths.extensionRegistry)
            val context = components.createContext(nameResolver, packageFqName, TypeTable(classProto.typeTable))
            val classId = JsMetaFileUtils.getClassId(file)
            return createTopLevelClassStub(classId, classProto, context)
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, nameResolver: NameResolver): ClsStubBuilderComponents {
        val classDataFinder = KotlinJavascriptClassDataFinder(nameResolver) { path ->
            file.parent.findChild(path.substringAfterLast("/"))?.inputStream
        }
        val annotationLoader = AnnotationLoaderForStubBuilderImpl(JsSerializerProtocol)
        return ClsStubBuilderComponents(classDataFinder, annotationLoader, file)
    }
}
