/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot

class AccessibleClassSnapshotImpl(override val classAbiHash: Long) : AccessibleClassSnapshot

object InaccessibleClassSnapshotImpl : InaccessibleClassSnapshot
