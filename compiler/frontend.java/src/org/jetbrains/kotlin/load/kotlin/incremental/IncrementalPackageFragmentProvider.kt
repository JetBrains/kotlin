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

package org.jetbrains.kotlin.load.kotlin.incremental

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

class IncrementalPackageFragmentProvider(
        sourceFiles: Collection<KtFile>,
        val moduleDescriptor: ModuleDescriptor,
        val storageManager: StorageManager,
        val deserializationComponents: DeserializationComponents,
        val incrementalCache: IncrementalCache,
        val target: TargetId
) : PackageFragmentProvider {
    val fqNameToSubFqNames = MultiMap<FqName, FqName>()
    val fqNameToPackageFragment = HashMap<FqName, PackageFragmentDescriptor>()
    val fqNamesToLoad: Set<FqName> = PackagePartClassUtils.getFilesWithCallables(sourceFiles).map { it.packageFqName }.toSet()

    init {
        fun createPackageFragment(fqName: FqName) {
            if (fqNameToPackageFragment.containsKey(fqName)) {
                return
            }

            if (!fqName.isRoot) {
                val parent = fqName.parent()
                createPackageFragment(parent)
                fqNameToSubFqNames.putValue(parent, fqName)
            }

            fqNameToPackageFragment[fqName] = IncrementalPackageFragment(fqName)
        }

        fqNamesToLoad.forEach(::createPackageFragment)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return fqNameToSubFqNames[fqName].orEmpty()
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return fqNameToPackageFragment[fqName].singletonOrEmptyList()
    }


    inner class IncrementalPackageFragment(fqName: FqName) : PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
        val target: TargetId
            get() = this@IncrementalPackageFragmentProvider.target

        fun getPackageFragmentForMultifileClass(multifileClassFqName: FqName): IncrementalMultifileClassPackageFragment? {
            val facadeInternalName = JvmClassName.byFqNameWithoutInnerClasses(multifileClassFqName).internalName
            val partsNames = incrementalCache.getStableMultifileFacadeParts(facadeInternalName) ?: return null
            return IncrementalMultifileClassPackageFragment(multifileClassFqName, partsNames)
        }

        override fun getMemberScope(): MemberScope = MemberScope.Empty

        inner class IncrementalMultifileClassPackageFragment(
                val multifileClassFqName: FqName,
                val partsNames: Collection<String>
        ) : PackageFragmentDescriptorImpl(moduleDescriptor, multifileClassFqName.parent()) {
            val memberScope = storageManager.createLazyValue {
                val partsData = partsNames.mapNotNull { internalName ->
                    incrementalCache.getPackagePartData(internalName)?.let { internalName to it }
                }
                if (partsData.isEmpty())
                    MemberScope.Empty
                else {
                    ChainedMemberScope(
                            "Member scope for incremental compilation: union of multifile class parts data for $multifileClassFqName",
                            partsData.map { createPackageScope(it.first, it.second, multifileClassFqName.asString()) }
                    )
                }
            }

            override fun getMemberScope(): MemberScope = memberScope()
        }

        fun createPackageScope(internalName: String, part: JvmPackagePartProto, facadeFqName: String?): DeserializedPackageMemberScope {
            val packageData = JvmProtoBufUtil.readPackageDataFrom(part.data, part.strings)
            return DeserializedPackageMemberScope(
                    this, packageData.packageProto, packageData.nameResolver,
                    JvmPackagePartSource(
                            JvmClassName.byInternalName(internalName),
                            facadeFqName?.let(JvmClassName::byFqNameWithoutInnerClasses)
                    ),
                    deserializationComponents, { listOf() }
            )
        }
    }
}
