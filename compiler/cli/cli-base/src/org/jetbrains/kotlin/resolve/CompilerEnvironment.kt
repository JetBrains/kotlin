/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.cfg.ControlFlowInformationProviderImpl
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance

object CompilerEnvironment : TargetEnvironment("Compiler") {
    override fun configure(container: StorageComponentContainer) {
        configureCompilerEnvironment(container)
        container.useInstance(ControlFlowInformationProviderImpl.Factory)
    }
}
