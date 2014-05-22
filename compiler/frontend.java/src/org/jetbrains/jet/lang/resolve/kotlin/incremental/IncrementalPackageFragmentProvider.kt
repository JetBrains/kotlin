/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin.incremental

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.util.containers.MultiMap
import java.util.HashMap
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers
import org.jetbrains.jet.descriptors.serialization.DescriptorFinder
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.descriptors.serialization.ClassId
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils
import java.util.Collections
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty

public class IncrementalPackageFragmentProvider(
        sourceFiles: Collection<JetFile>,
        val module: ModuleDescriptor,
        val storageManager: StorageManager,
        val deserializers: Deserializers,
        val incrementalCache: IncrementalCache,
        val moduleId: String,
        val javaDescriptorResolver: JavaDescriptorResolver

) : PackageFragmentProvider {

    val memberFilter = CliSourcesMemberFilter(sourceFiles)
    val fqNameToSubFqNames = MultiMap<FqName, FqName>()
    val fqNameToPackageFragment = HashMap<FqName, PackageFragmentDescriptor>()

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

        for (source in sourceFiles) {
            if (source.getDeclarations().any { it is JetProperty || it is JetNamedFunction }) {
                createPackageFragment(source.getPackageFqName())
            }
        }
    }

    override fun getSubPackagesOf(fqName: FqName): Collection<FqName> {
        return fqNameToSubFqNames[fqName].orEmpty()
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return fqNameToPackageFragment[fqName].singletonOrEmptyList()
    }


    public inner class IncrementalPackageFragment(fqName: FqName) : PackageFragmentDescriptorImpl(module, fqName) {
        val descriptorFinder = object : DescriptorFinder {
            override fun findClass(classId: ClassId): ClassDescriptor? =
                    javaDescriptorResolver.resolveClass(DeserializedResolverUtils.kotlinFqNameToJavaFqName(classId.asSingleFqName()))

            override fun getClassNames(packageName: FqName): Collection<Name> {
                return Collections.emptySet()
            }
        }

        val _memberScope: NotNullLazyValue<JetScope> = storageManager.createLazyValue {
            val packageDataBytes = incrementalCache.getPackageData(moduleId, fqName)
            if (packageDataBytes == null) {
                JetScope.EMPTY
            }
            else {
                val packageData = JavaProtoBufUtil.readPackageDataFrom(packageDataBytes)
                DeserializedPackageMemberScope(
                        storageManager,
                        this,
                        deserializers,
                        memberFilter,
                        descriptorFinder,
                        packageData
                )
            }

        }

        override fun getMemberScope(): JetScope {
            return _memberScope()
        }
    }
}

