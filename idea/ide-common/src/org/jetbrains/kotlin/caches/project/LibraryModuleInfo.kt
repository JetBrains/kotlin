/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.project

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.resolve.TargetPlatform

interface LibraryModuleInfo : ModuleInfo {
    override val platform: TargetPlatform

    fun getLibraryRoots(): Collection<String>
}
