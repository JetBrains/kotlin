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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.util.containers.MultiMap
import java.util.HashMap
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.*
import org.jetbrains.kotlin.serialization.jvm.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents

public class IncrementalPackageFragmentProvider(
        sourceFiles: Collection<JetFile>,
        val module: ModuleDescriptor,
        val storageManager: StorageManager,
        val deserializationComponents: DeserializationComponents,
        val incrementalCache: IncrementalCache,
        val moduleId: String
) : PackageFragmentProvider {

    val obsoletePackageParts = incrementalCache.getObsoletePackageParts().toSet()
    val fqNameToSubFqNames = MultiMap<FqName, FqName>()
    val fqNameToPackageFragment = HashMap<FqName, PackageFragmentDescriptor>()
    val fqNamesToLoad: Set<FqName>

    ;{
        fun createPackageFragment(fqName: FqName) {
            if (fqNameToPackageFragment.containsKey(fqName)) {
                return
            }

            if (!fqName.isRoot()) {
                val parent = fqName.parent()
                createPackageFragment(parent)
                fqNameToSubFqNames.putValue(parent, fqName)
            }

            fqNameToPackageFragment[fqName] = IncrementalPackageFragment(fqName)
        }

        fqNamesToLoad = (
                obsoletePackageParts.map { JvmClassName.byInternalName(it).getPackageFqName() }
                + PackagePartClassUtils.getPackageFilesWithCallables(sourceFiles).map { it.getPackageFqName() }
        ).toSet()

        fqNamesToLoad.forEach { createPackageFragment(it) }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return fqNameToSubFqNames[fqName].orEmpty()
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return fqNameToPackageFragment[fqName].singletonOrEmptyList()
    }


    public inner class IncrementalPackageFragment(fqName: FqName) : PackageFragmentDescriptorImpl(module, fqName) {
        public val moduleId: String
            get() = this@IncrementalPackageFragmentProvider.moduleId

        val memberScope: NotNullLazyValue<JetScope> = storageManager.createLazyValue {
            if (fqName !in fqNamesToLoad) {
                JetScope.Empty
            }
            else {
                val packageDataBytes = incrementalCache.getPackageData(fqName.asString())
                if (packageDataBytes == null) {
                    JetScope.Empty
                }
                else {
                    IncrementalPackageScope(JvmProtoBufUtil.readPackageDataFrom(packageDataBytes))
                }
            }
        }

        override fun getMemberScope(): JetScope = memberScope()

        private inner class IncrementalPackageScope(val packageData: PackageData) : DeserializedPackageMemberScope(
                this@IncrementalPackageFragment, packageData.getPackageProto(), packageData.getNameResolver(), deserializationComponents,
                { listOf() }
        ) {
            override fun filteredMemberProtos(allMemberProtos: Collection<ProtoBuf.Callable>): Collection<ProtoBuf.Callable> {
                return allMemberProtos
                        .filter {
                            member ->
                            if (member.hasExtension(JvmProtoBuf.implClassName)) {
                                val shortName = packageData.getNameResolver().getName(member.getExtension(JvmProtoBuf.implClassName)!!)
                                val internalName = JvmClassName.byFqNameWithoutInnerClasses(fqName.child(shortName)).getInternalName()
                                internalName !in obsoletePackageParts
                            }
                            else {
                                true
                            }
                        }
            }
        }
    }
}

