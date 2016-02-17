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

package org.jetbrains.kotlin.idea.decompiler.builtIns

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsClassDataFinder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope

class KotlinBuiltInDeserializerForDecompiler(
        packageDirectory: VirtualFile,
        packageFqName: FqName,
        private val proto: BuiltInsProtoBuf.BuiltIns,
        private val nameResolver: NameResolver
) : DeserializerForDecompilerBase(packageDirectory, packageFqName) {
    override val targetPlatform: TargetPlatform get() = TargetPlatform.Default

    override val deserializationComponents = DeserializationComponents(
            storageManager, moduleDescriptor, BuiltInsClassDataFinder(proto, nameResolver),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, BuiltInSerializerProtocol), packageFragmentProvider,
            ResolveEverythingToKotlinAnyLocalClassResolver(targetPlatform.builtIns), LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING, FlexibleTypeCapabilitiesDeserializer.ThrowException, ClassDescriptorFactory.EMPTY
    )

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        assert(facadeFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }

        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(facadeFqName), proto.`package`, nameResolver, packagePartSource = null,
                components = deserializationComponents
        ) { emptyList() }

        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinBuiltInDeserializerForDecompiler::class.java)
    }
}
