/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.collections

import org.jetbrains.kotlin.utils.SmartList

public inline fun <E> buildSmartList(build: MutableList<E>.() -> Unit): List<E> =
    SmartList<E>().apply(build)