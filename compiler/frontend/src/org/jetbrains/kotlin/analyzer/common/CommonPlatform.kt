/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer.common

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.resolve.*

private object CommonPlatformConfigurator : PlatformConfiguratorBase() {
    override fun configureModuleComponents(container: StorageComponentContainer) {}
}

object CommonPlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = CommonPlatformConfigurator
    override val defaultImportsProvider: DefaultImportsProvider = CommonDefaultImportsProvider

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.AFTER_SDK
}
