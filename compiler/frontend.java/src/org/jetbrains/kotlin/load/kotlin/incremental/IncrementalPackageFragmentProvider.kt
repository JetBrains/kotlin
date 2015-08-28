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
import org.apache.log4j.Logger
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*
import kotlin.reflect.jvm.*

public class IncrementalPackageFragmentProvider(
        sourceFiles: Collection<JetFile>,
        val module: ModuleDescriptor,
        val storageManager: StorageManager,
        val deserializationComponents: DeserializationComponents,
        val incrementalCache: IncrementalCache,
        val moduleId: String
) : PackageFragmentProvider {

    private companion object {
        private val LOG = Logger.getLogger(IncrementalPackageFragmentProvider::class.java)
    }

    val obsoletePackageParts = incrementalCache.getObsoletePackageParts().toSet()
    val fqNameToSubFqNames = MultiMap<FqName, FqName>()
    val fqNameToPackageFragment = HashMap<FqName, PackageFragmentDescriptor>()
    val fqNamesToLoad: Set<FqName>

    init {
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
                + PackagePartClassUtils.getFilesWithCallables(sourceFiles).map { it.getPackageFqName() }
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
                fun getPackagePart(callable: ProtoBuf.Callable) =
                        callable.getExtension(JvmProtoBuf.implClassName)?.let { packageData.getNameResolver().getName(it) }

                fun shouldSkipPackagePart(name: Name) =
                        JvmClassName.byFqNameWithoutInnerClasses(fqName.child(name)).getInternalName() in obsoletePackageParts

                if (LOG.isDebugEnabled()) {
                    val allPackageParts = allMemberProtos
                            .map (::getPackagePart)
                            .filterNotNull()
                            .toSet()
                    val skippedPackageParts = allPackageParts.filter { shouldSkipPackagePart(it) }

                    LOG.debug("Loading incremental package fragment for package '$fqName'," +
                              " all package parts: $allPackageParts, skipped parts: $skippedPackageParts")
                }

                return allMemberProtos.filter { getPackagePart(it)?.let { !shouldSkipPackagePart(it) } ?: true }
            }
        }
    }
}

