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

import org.jetbrains.kotlin.config.LanguageFeature.DefaultImportOfPackageKotlinComparisons
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.SinceKotlinAccessibility
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class DefaultImportProvider(
    storageManager: StorageManager,
    private val targetPlatform: TargetPlatform,
    private val languageVersionSettings: LanguageVersionSettings
) {
    val defaultImports: List<ImportPath> by storageManager.createLazyValue {
        targetPlatform.getDefaultImports(languageVersionSettings.supportsFeature(DefaultImportOfPackageKotlinComparisons))
    }

    val allDefaultImports: List<ImportPath> by storageManager.createLazyValue {
        defaultImports + targetPlatform.defaultLowPriorityImports
    }
}
