/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

fun <L> Project.syncPublisherWithDisposeCheck(topic: Topic<L>) =
    if (isDisposed) throw ProcessCanceledException() else messageBus.syncPublisher(topic)