/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer.common

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.storage.StorageManager

private object CommonPlatformConfigurator : PlatformConfiguratorBase() {
    override fun configureModuleComponents(container: StorageComponentContainer) {}
}

object CommonPlatformCompilerServices : PlatformDependentCompilerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
}