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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.backend.konan.KonanPlatformConfigurator
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class KonanBuiltIns: KotlinBuiltIns {
    constructor(storageManager: StorageManager) : super(storageManager)

    constructor(storageManager: StorageManager, withModule: Boolean) : this(storageManager) {
        if (withModule) createBuiltInsModule()
    }
}

object KonanPlatform : TargetPlatform("Konan") {
    override val multiTargetPlatform = MultiTargetPlatform.Specific(platformName)
    override fun getDefaultImports(languageVersionSettings: LanguageVersionSettings): List<ImportPath> =
            Default.getDefaultImports(languageVersionSettings) + listOf(
                    ImportPath("kotlin.*"),
                    ImportPath("kotlin.collections.*"),
                    ImportPath("kotlin.io.*"),
                    ImportPath("konan.*")
            )

    override val platformConfigurator: PlatformConfigurator = KonanPlatformConfigurator

    val builtIns: KonanBuiltIns = KonanBuiltIns(LockBasedStorageManager.NO_LOCKS, false)
}
