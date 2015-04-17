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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.navigation.JsMetaFileUtils
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptAnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializedResourcePaths
import org.jetbrains.kotlin.serialization.js.toPackageData
import java.io.ByteArrayInputStream

public class KotlinJavaScriptDeserializerForDecompiler(
        classFile: VirtualFile
) : DeserializerForDecompilerBase(classFile.getParent()!!, JsMetaFileUtils.getPackageFqName(classFile)) {

    private val nameResolver = run {
        val moduleDirectory = JsMetaFileUtils.getModuleDirectory(packageDirectory)
        val stringsFileName = KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(directoryPackageFqName)
        val stringsFile = moduleDirectory.findFileByRelativePath(stringsFileName)
        assert(stringsFile != null, "strings file not found: $stringsFileName")
        NameResolver.read(ByteArrayInputStream(stringsFile!!.contentsToByteArray(false)))
    }

    private val metaFileFinder = DirectoryBasedKotlinJavaScriptMetaFileFinder(packageDirectory, directoryPackageFqName, nameResolver)

    override val classDataFinder = DirectoryBasedKotlinJavaScriptDataFinder(metaFileFinder, LOG)

    override val annotationAndConstantLoader = KotlinJavascriptAnnotationAndConstantLoader(moduleDescriptor)

    override val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver, FlexibleTypeCapabilitiesDeserializer.Dynamic
    )

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName, "Was called for $packageFqName but only $directoryPackageFqName is expected.")
        val file = metaFileFinder.findKotlinJavascriptMetaFile(PackageClassUtils.getPackageClassId(packageFqName))
        if (file == null) {
            LOG.error("Could not read data for $packageFqName")
            return emptyList()
        }

        val content = file.contentsToByteArray(false)
        val packageData = content.toPackageData(nameResolver)

        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                packageData.getPackageProto(),
                packageData.getNameResolver(),
                deserializationComponents
        ) { emptyList() }
        return membersScope.getDescriptors()
    }

    companion object {
        private val LOG = Logger.getInstance(javaClass<KotlinJavaScriptDeserializerForDecompiler>())
    }
}
