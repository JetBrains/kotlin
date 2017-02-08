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
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.addToStdlib.check

class DefaultImportProvider(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        private val targetPlatform: TargetPlatform,
        private val languageVersionSettings: LanguageVersionSettings
) {
    val defaultImports: List<ImportPath>
            by storageManager.createLazyValue { targetPlatform.getDefaultImports(languageVersionSettings) }

    val excludedImports: List<FqName> by storageManager.createLazyValue {
        val packagesWithAliases = listOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, KotlinBuiltIns.TEXT_PACKAGE_FQ_NAME)
        val builtinTypeAliases = moduleDescriptor.allDependencyModules
                .flatMap { dependencyModule ->
                    packagesWithAliases.map(dependencyModule::getPackage).flatMap {
                        it.memberScope.getContributedDescriptors(DescriptorKindFilter.TYPE_ALIASES).filterIsInstance<TypeAliasDescriptor>()
                    }
                }
                .filter { it.checkSinceKotlinVersionAccessibility(languageVersionSettings) }


        val nonKotlinDefaultImportedPackages =
                defaultImports
                        .filter { it.isAllUnder }
                        .mapNotNull {
                            it.fqnPart().check { !it.isSubpackageOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) }
                        }
        val nonKotlinAliasedTypeFqNames =
                builtinTypeAliases
                        .mapNotNull { it.expandedType.constructor.declarationDescriptor?.fqNameSafe }
                        .filter { nonKotlinDefaultImportedPackages.any(it::isChildOf) }

        nonKotlinAliasedTypeFqNames + targetPlatform.excludedImports
    }
}
