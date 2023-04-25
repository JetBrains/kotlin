/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.config

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.load.java.JavaClassesTracker

object ClassicFrontendSpecificJvmConfigurationKeys {
    @JvmField
    val JAVA_CLASSES_TRACKER: CompilerConfigurationKey<JavaClassesTracker> = CompilerConfigurationKey.create("Java classes tracker")
}
