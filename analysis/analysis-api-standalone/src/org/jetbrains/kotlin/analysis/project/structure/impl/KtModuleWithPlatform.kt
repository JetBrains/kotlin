/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import org.jetbrains.kotlin.platform.TargetPlatform

internal interface KtModuleWithPlatform {
    val platform: TargetPlatform
}
