/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: ${javaClass.name}")
