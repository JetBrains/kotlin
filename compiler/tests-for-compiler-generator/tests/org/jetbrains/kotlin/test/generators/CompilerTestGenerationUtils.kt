/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import java.io.File

internal fun skipSpecificFileForFirDiagnosticTest(onlyTypealiases: Boolean): (File) -> Boolean {
    return when (onlyTypealiases) {
        true -> {
            { !it.readText().contains("typealias") }
        }
        false -> {
            { false }
        }
    }
}
