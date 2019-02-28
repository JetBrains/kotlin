/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.daemon.common.experimental.CompileServiceServerSide
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*

interface CompileServiceClientSide : CompileServiceAsync, Client<CompileServiceServerSide> {
    override val serverPort: Int
}