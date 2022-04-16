/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File

/**
 * For proper initialization of idea services those two properties should
 *   be set in environment of test. You can setup them manually via build
 *   system of run configurations or just `initIdeaConfiguration` before
 *   running tests using abilities of core test framework you use
 */
fun initIdeaConfiguration() {
    System.setProperty("idea.home", computeHomeDirectory())
    System.setProperty("idea.ignore.disabled.plugins", "true")
}

private fun computeHomeDirectory(): String {
    val userDir = System.getProperty("user.dir")
    return File(userDir ?: ".").canonicalPath
}
