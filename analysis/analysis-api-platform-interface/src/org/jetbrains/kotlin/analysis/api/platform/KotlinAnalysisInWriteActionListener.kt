/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

import com.intellij.util.messages.Topic

/**
 * A listener for events which happen during an [analyze][org.jetbrains.kotlin.analysis.api.analyze] call *in a write action*.
 *
 * Normally, analysis during write actions is not allowed. Nonetheless, it is possible, and such analysis poses some unique challenges to
 * cache integrity, as analysis usually operates on an immutable context (read action) and not *during* or *between* modifications. The
 * purpose of this listener is to allow components to react when a clean slate is required upon entering or leaving analysis.
 *
 * @see org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction
 */
public interface KotlinAnalysisInWriteActionListener {
    /**
     * This event is published when analysis is entered during a write action. It is published before the action of
     * [analyze][org.jetbrains.kotlin.analysis.api.analyze] is executed.
     *
     * @see KotlinAnalysisInWriteActionListener
     */
    public fun onEnteringAnalysisInWriteAction()

    /**
     * This event is published after leaving analysis during a write action. It is published after the action of
     * [analyze][org.jetbrains.kotlin.analysis.api.analyze] has been executed.
     *
     * @see KotlinAnalysisInWriteActionListener
     */
    public fun afterLeavingAnalysisInWriteAction()

    public companion object {
        public val TOPIC: Topic<KotlinAnalysisInWriteActionListener> = Topic(
            KotlinAnalysisInWriteActionListener::class.java,
            Topic.BroadcastDirection.TO_CHILDREN,
            true,
        )
    }
}
