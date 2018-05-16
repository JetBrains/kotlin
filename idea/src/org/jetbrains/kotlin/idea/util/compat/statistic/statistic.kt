/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncompatibleAPI")

package org.jetbrains.kotlin.idea.util.compat.statistic

/**
 * Should be dropped after abandoning 172.
 * BUNCH: 173
 */
typealias AbstractProjectsUsagesCollector = com.intellij.internal.statistic.AbstractProjectsUsagesCollector

/**
 * Should be dropped after abandoning 172.
 * BUNCH: 173
 */
fun getEnumUsage(key: String, value: Enum<*>?) = com.intellij.internal.statistic.utils.getEnumUsage(key, value)

/**
 * Should be dropped after abandoning 172.
 * BUNCH: 173
 */
fun getBooleanUsage(key: String, value: Boolean) = com.intellij.internal.statistic.utils.getBooleanUsage(key, value)
