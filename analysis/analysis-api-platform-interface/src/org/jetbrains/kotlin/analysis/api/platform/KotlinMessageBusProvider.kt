/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics

/**
 * [KotlinMessageBusProvider] allows Analysis API implementations to provide a custom [MessageBus]. When subscribing to or publishing to
 * Analysis API topics ([KotlinModificationTopics]), the message bus provided by [getMessageBus] should be used, not the [Project]'s message bus.
 */
public interface KotlinMessageBusProvider : KotlinPlatformComponent {
    public fun getMessageBus(): MessageBus

    public companion object {
        public fun getInstance(project: Project): KotlinMessageBusProvider =
            project.getService(KotlinMessageBusProvider::class.java)
    }
}

/**
 * The [MessageBus] used to subscribe to and publish to Analysis API topics ([KotlinModificationTopics]).
 */
public val Project.analysisMessageBus: MessageBus
    get() = KotlinMessageBusProvider.getInstance(this).getMessageBus()
