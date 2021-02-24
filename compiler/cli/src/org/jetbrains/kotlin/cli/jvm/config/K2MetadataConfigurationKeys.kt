/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.config

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object K2MetadataConfigurationKeys {
    val FRIEND_PATHS = CompilerConfigurationKey.create<List<String>>("friend module paths")
    val REFINES_PATHS = CompilerConfigurationKey.create<List<String>>("refined module paths")
}