/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.platform.TargetPlatform

interface LibraryModuleInfo : ModuleInfo {
    override val platform: TargetPlatform

    fun getLibraryRoots(): Collection<String>
}
