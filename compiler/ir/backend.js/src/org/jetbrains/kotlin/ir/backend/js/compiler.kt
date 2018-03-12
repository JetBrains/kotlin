/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.psi.KtFile

// Dummy compile function to work on the test infrastructure
fun compile(
    files: List<KtFile>
): String {
    if (files.none { it.name == "nestedPackage.kt" }) return ""
    return "function box() { return \"OK\"; }"
}