/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import kotlin.reflect.KClass

sealed class BuildTime<T : BuildTime<T>>(open val parent: T?, val readableString: String, val name: String) : Serializable {
    //TODO make it Thread safe!!!!!!
    protected val children = mutableListOf<T>()

    fun children(): List<T> = children

    //TODO do we need to add child manually???
}

fun <T : BuildTime<T>> getAllMetricsByType(buildTimeClass: KClass<T>): List<T> =
    buildTimeClass.sealedSubclasses.mapNotNull { it.objectInstance }.flatMap { it.children() + it }

fun getAllMetrics() = BuildTime::class.sealedSubclasses.mapNotNull { it.objectInstance }.flatMap { it.children() + it }