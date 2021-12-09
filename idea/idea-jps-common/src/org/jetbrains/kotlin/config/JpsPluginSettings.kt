/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.cli.common.arguments.Freezable

class JpsPluginSettings : Freezable() {
    @Suppress("unused") // Used in Kotlin plugin
    var version: String by FreezableVar("")
}
