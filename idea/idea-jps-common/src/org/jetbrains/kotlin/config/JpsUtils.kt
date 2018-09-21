/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

private const val IDE_EVENT_QUEUE_CLASS_NAME = "com.intellij.ide.IdeEventQueue"

val isJps: Boolean by lazy {
    val ideEventQueueClassPath = IDE_EVENT_QUEUE_CLASS_NAME.replace('.', '/') + ".class"
    {}.javaClass.classLoader.getResource(ideEventQueueClassPath) == null
}