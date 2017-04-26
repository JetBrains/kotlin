/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager

val STDLIB_MODULE_NAME = Name.special("<stdlib>")

fun ModuleDescriptor.isStdlib(): Boolean {
    return name == STDLIB_MODULE_NAME
}

private val nativePtrName = "NativePtr"

class KonanBuiltIns(storageManager: StorageManager) : KotlinBuiltIns(storageManager) {
    override fun getClassDescriptorFactories() =
            super.getClassDescriptorFactories() + KonanBuiltInClassDescriptorFactory(storageManager, builtInsModule)

    object FqNames {
        val packageName = FqName("konan.internal")

        val nativePtr = packageName.child(Name.identifier(nativePtrName)).toUnsafe()
    }

    private val packageScope by lazy { builtInsModule.getPackage(FqNames.packageName).memberScope }

    val nativePtr by lazy { packageScope.getContributedClassifier(nativePtrName) as ClassDescriptor }

    val nativePtrPlusLong by lazy { nativePtr.unsubstitutedMemberScope.getContributedFunctions("plus").single() }
    val nativePtrToLong   by lazy { nativePtr.unsubstitutedMemberScope.getContributedFunctions("toLong").single() }
    val getNativeNullPtr  by lazy { packageScope.getContributedFunctions("getNativeNullPtr").single() }

    private fun MemberScope.getContributedClassifier(name: String) =
            this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

    private fun MemberScope.getContributedFunctions(name: String) =
            this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)
}

object KonanPlatform : TargetPlatform("Konan") {
    override val multiTargetPlatform = MultiTargetPlatform.Specific(platformName)
    override fun getDefaultImports(includeKotlinComparisons: Boolean): List<ImportPath> {
       return Default.getDefaultImports(includeKotlinComparisons) + listOf(
                ImportPath.fromString("konan.*")
        )
    }

    override val platformConfigurator: PlatformConfigurator = KonanPlatformConfigurator
}
