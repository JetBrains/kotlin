/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Action
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.AbstractCopyTask

fun AbstractCopyTask.lazyFrom(
    filesProvider: () -> Any?
): CopySourceSpec = from(project.provider{ filesProvider() })

fun AbstractCopyTask.lazyFrom(
    filesProvider: () -> Any?,
    configureAction: CopySpec.() -> Unit
): CopySourceSpec = from(project.provider { filesProvider() }, Action { configureAction() })
