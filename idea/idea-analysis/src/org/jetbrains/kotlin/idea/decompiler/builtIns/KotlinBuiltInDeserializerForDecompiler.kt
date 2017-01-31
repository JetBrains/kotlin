/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsClassDataFinder
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DeserializerForDecompilerBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolveEverythingToKotlinAnyLocalClassifierResolver
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope

class KotlinBuiltInDeserializerForDecompiler(
        packageFqName: FqName,
        private val proto: ProtoBuf.PackageFragment,
        private val nameResolver: NameResolver
) : DeserializerForDecompilerBase(packageFqName) {
    override val targetPlatform: TargetPlatform get() = TargetPlatform.Default
    override val builtIns: KotlinBuiltIns get() = DefaultBuiltIns.Instance

    override val deserializationComponents: DeserializationComponents

    init {
        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        deserializationComponents = DeserializationComponents(
                storageManager, moduleDescriptor, DeserializationConfiguration.Default, BuiltInsClassDataFinder(proto, nameResolver),
                AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol), packageFragmentProvider,
                ResolveEverythingToKotlinAnyLocalClassifierResolver(builtIns), LoggingErrorReporter(LOG),
                LookupTracker.DO_NOTHING, FlexibleTypeDeserializer.ThrowException, emptyList(), notFoundClasses
        )
    }

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        assert(facadeFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }

        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(facadeFqName), proto.`package`, nameResolver, containerSource = null,
                components = deserializationComponents
        ) { emptyList() }

        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinBuiltInDeserializerForDecompiler::class.java)
    }
}
