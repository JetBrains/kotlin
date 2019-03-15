/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class FirTestModuleInfo(
    override val name: Name = Name.identifier("TestModule"),
    val dependencies: MutableList<ModuleInfo> = mutableListOf(),
    override val platform: TargetPlatform = JvmPlatform
) : ModuleInfo {
    override fun dependencies(): List<ModuleInfo> = dependencies
}