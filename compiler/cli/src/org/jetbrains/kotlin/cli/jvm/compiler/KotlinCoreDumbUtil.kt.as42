/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.DumbUtil

@Suppress("UnstableApiUsage")
class KotlinCoreDumbUtil : DumbUtil {
    override fun <T : Any?> filterByDumbAwarenessHonoringIgnoring(collection: Collection<T>): List<T> =
        when (collection) {
            is List<T> -> collection
            else -> ArrayList(collection)
        }

    override fun mayUseIndices(): Boolean = false
}