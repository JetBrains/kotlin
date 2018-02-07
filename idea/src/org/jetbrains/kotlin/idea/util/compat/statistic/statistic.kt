/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util.compat.statistic

typealias AbstractProjectsUsagesCollector = com.intellij.internal.statistic.AbstractApplicationUsagesCollector

fun getEnumUsage(key: String, value: Enum<*>?) = com.intellij.internal.statistic.getEnumUsage(key, value)
