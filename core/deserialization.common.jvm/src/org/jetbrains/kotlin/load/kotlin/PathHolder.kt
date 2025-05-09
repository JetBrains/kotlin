/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

/**
 * This class serves as a wrapper to maintain JDK 6 compatibility for kotlin-reflect.
 * It allows storing Path objects without direct dependency on java.nio.file.Path,
 * which is only available since JDK 7.
 */
class PathHolder(val path: Any)
