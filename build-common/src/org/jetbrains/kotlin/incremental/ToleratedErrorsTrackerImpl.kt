/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.ToleratedErrorsTracker
import java.io.File

class ToleratedErrorsTrackerImpl : ToleratedErrorsTracker {
    val files: Set<File>
        field = LinkedHashSet<File>()

    override fun report(filePath: String) {
        files.add(File(filePath).normalize())
    }
}
