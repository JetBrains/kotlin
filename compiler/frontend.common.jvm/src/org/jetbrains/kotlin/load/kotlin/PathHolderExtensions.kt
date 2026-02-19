/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import java.nio.file.Path

/**
 * Convert PathHolder to java.nio.file.Path in modules that target JDK 7+.
 * PathHolder itself keeps JDK 6 compatibility in modules like kotlin-reflect
 * by avoiding a direct reference to java.nio.file.Path.
 */
fun PathHolder.asNioPath(): Path? = path as? Path
