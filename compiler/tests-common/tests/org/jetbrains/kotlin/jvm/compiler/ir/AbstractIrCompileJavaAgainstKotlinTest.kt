/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.ir

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractCompileJavaAgainstKotlinTest

abstract class AbstractIrCompileJavaAgainstKotlinTest : AbstractCompileJavaAgainstKotlinTest() {
    override fun updateConfiguration(configuration: CompilerConfiguration) = configuration.put(JVMConfigurationKeys.IR, true)
}
