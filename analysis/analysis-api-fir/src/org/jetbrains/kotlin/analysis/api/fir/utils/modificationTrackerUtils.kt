/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.utils.trackers.CompositeModificationTracker

public fun List<ModificationTracker>.createCompositeModificationTracker(): ModificationTracker =
    CompositeModificationTracker.createFlattened(this)
