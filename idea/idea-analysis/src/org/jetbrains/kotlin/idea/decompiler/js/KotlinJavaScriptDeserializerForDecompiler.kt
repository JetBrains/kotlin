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
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.js.*
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

    private val metaFileFinder = DirectoryBasedKotlinJavaScriptMetaFileFinder(packageDirectory, directoryPackageFqName, nameResolver)

    private val classDataFinder = DirectoryBasedKotlinJavaScriptDataFinder(metaFileFinder, LOG)

    private val annotationAndConstantLoader = KotlinJavascriptAnnotationAndConstantLoader(moduleDescriptor)

    override val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver(targetPlatform.builtIns), LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING, FlexibleTypeCapabilitiesDeserializer.Dynamic, ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): Collection<DeclarationDescriptor> {
        val packageFqName = facadeFqName.parent()
        assert(packageFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }
        val file = metaFileFinder.findKotlinJavascriptMetaFile(ClassId.topLevel(facadeFqName))
        if (file == null) {
            LOG.error("Could not read data for $facadeFqName")
            return emptyList()
        }

        val content = file.contentsToByteArray(false)
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName), content.toPackageProto(), nameResolver, deserializationComponents
        ) { emptyList() }
        return membersScope.getContributedDescriptors()
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinJavaScriptDeserializerForDecompiler::class.java)
    }
}

class DirectoryBasedKotlinJavaScriptMetaFileFinder(
        val packageDirectory: VirtualFile,
        val directoryPackageFqName: FqName,
        val nameResolver: NameResolver
) {
    fun findKotlinJavascriptMetaFile(classId: ClassId): VirtualFile? {
        if (classId.getPackageFqName() != directoryPackageFqName) return null

        val targetName = classId.getRelativeClassName().pathSegments().joinToString(".", postfix = "." + KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION)
        return packageDirectory.findChild(targetName)
    }
}

class DirectoryBasedKotlinJavaScriptDataFinder(
        val classFinder: DirectoryBasedKotlinJavaScriptMetaFileFinder,
        val log: Logger
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassDataWithSource? {
        val file = classFinder.findKotlinJavascriptMetaFile(classId) ?: return null

        val content = file.contentsToByteArray(false)
        return ClassDataWithSource(ClassData(classFinder.nameResolver, content.toClassProto()))
    }
}
