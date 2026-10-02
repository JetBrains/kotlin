/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import org.jetbrains.kotlin.platform.TargetPlatform

internal interface KtModuleWithPlatform {
    val targetPlatform: TargetPlatform
}
