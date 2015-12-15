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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.common.DirectoryBasedClassDataFinder
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializedResourcePaths
import java.io.ByteArrayInputStream

public class KotlinJavaScriptDeserializerForDecompiler(
        classFile: VirtualFile
) : DeserializerForDecompilerBase(classFile.parent!!, JsMetaFileUtils.getPackageFqName(classFile)) {

    private val nameResolver = run {
        val moduleDirectory = JsMetaFileUtils.getModuleDirectory(packageDirectory)
        val stringsFileName = KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(directoryPackageFqName)
        val stringsFile = moduleDirectory.findFileByRelativePath(stringsFileName)
        assert(stringsFile != null) { "strings file not found: $stringsFileName" }
        NameResolverImpl.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))
    }

    override val targetPlatform: TargetPlatform get() = JsPlatform

    private val finder = DirectoryBasedClassDataFinder(packageDirectory, directoryPackageFqName, nameResolver, KotlinJavascriptSerializedResourcePaths)

    private val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(moduleDescriptor, JsSerializerProtocol)

    override val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, finder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver(targetPlatform.builtIns), LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING, FlexibleTypeCapabilitiesDeserializer.Dynamic, ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        val packageFqName = facadeFqName.parent()
        return getDescriptorsFromPackageFile(packageFqName, KotlinJavascriptSerializedResourcePaths, LOG, nameResolver)
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinJavaScriptDeserializerForDecompiler::class.java)
    }
}
