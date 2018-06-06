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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature.DefaultImportOfPackageKotlinComparisons
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.SinceKotlinAccessibility
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class DefaultImportProvider(
    storageManager: StorageManager,
    moduleDescriptor: ModuleDescriptor,
    private val targetPlatform: TargetPlatform,
    private val languageVersionSettings: LanguageVersionSettings
) {
    companion object {
        private val PACKAGES_WITH_ALIASES = listOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, KotlinBuiltIns.TEXT_PACKAGE_FQ_NAME)

        private fun ModuleDescriptor.findTypeAliasesInPackages(packages: Collection<FqName>): Collection<TypeAliasDescriptor> {
            val result = mutableListOf<TypeAliasDescriptor>()

            for (dependencyModuleDescriptor in allDependencyModules) {
                if (dependencyModuleDescriptor !is ModuleDescriptorImpl) continue

                for (packageFqName in packages) {
                    dependencyModuleDescriptor.packageFragmentProviderForContent.getPackageFragments(packageFqName)
                        .flatMapTo(result) { packageFragmentDescriptor ->
                            packageFragmentDescriptor.getMemberScope()
                                .getContributedDescriptors(DescriptorKindFilter.TYPE_ALIASES)
                                .filterIsInstance<TypeAliasDescriptor>()
                        }
                }
            }

            return result
        }
    }

    val defaultImports: List<ImportPath> by storageManager.createLazyValue {
        targetPlatform.getDefaultImports(languageVersionSettings.supportsFeature(DefaultImportOfPackageKotlinComparisons))
    }

    val excludedImports: List<FqName> by storageManager.createLazyValue {
        val builtinTypeAliases =
            moduleDescriptor.findTypeAliasesInPackages(PACKAGES_WITH_ALIASES)
                .filter { it.checkSinceKotlinVersionAccessibility(languageVersionSettings) == SinceKotlinAccessibility.Accessible }

        val nonKotlinDefaultImportedPackages =
            defaultImports
                .filter { it.isAllUnder }
                .mapNotNull {
                    it.fqName.takeUnless { it.isSubpackageOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) }
                }
        val nonKotlinAliasedTypeFqNames =
            builtinTypeAliases
                .mapNotNull { it.expandedType.constructor.declarationDescriptor?.fqNameSafe }
                .filter { nonKotlinDefaultImportedPackages.any(it::isChildOf) }

        nonKotlinAliasedTypeFqNames + targetPlatform.excludedImports
    }
}
