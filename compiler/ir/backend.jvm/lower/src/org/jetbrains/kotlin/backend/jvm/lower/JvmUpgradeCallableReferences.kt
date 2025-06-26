/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext

@PhaseDescription("JvmUpgradeCallableReferences")
internal class JvmUpgradeCallableReferences(context: JvmBackendContext) : UpgradeCallableReferences(
    context = context,
    upgradeFunctionReferencesAndLambdas = true,
    upgradePropertyReferences = true,
    upgradeLocalDelegatedPropertyReferences = true,
    upgradeSamConversions = true
)
