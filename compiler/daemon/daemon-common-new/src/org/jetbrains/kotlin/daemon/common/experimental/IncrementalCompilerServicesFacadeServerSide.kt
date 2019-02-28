/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacadeAsync
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.File


interface IncrementalCompilerServicesFacadeServerSide : IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseServerSide