/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import org.jetbrains.kotlin.analysis.providers.KotlinMessageBusProvider

/**
 * Provides the [project]'s [MessageBus] as the Analysis API message bus. This is the default implementation for both the standalone and the
 * IDE Analysis API.
 *
 * [KotlinMessageBusProvider] exists so that this default may change in the future without breaking the API. Hence, it should not be assumed
 * that the message bus provided by [KotlinMessageBusProvider] will always be equal to the project's message bus.
 */
public class KotlinProjectMessageBusProvider(private val project: Project) : KotlinMessageBusProvider {
    override fun getMessageBus(): MessageBus = project.messageBus
}
