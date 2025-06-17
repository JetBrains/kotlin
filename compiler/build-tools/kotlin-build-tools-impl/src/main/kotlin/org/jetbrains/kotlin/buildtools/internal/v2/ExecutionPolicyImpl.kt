/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.internal.v2.OptionsDelegate

object InProcessExecutionPolicy : ExecutionPolicy

class DaemonExecutionPolicy(val daemonJvmArgs: List<String>) : ExecutionPolicy
