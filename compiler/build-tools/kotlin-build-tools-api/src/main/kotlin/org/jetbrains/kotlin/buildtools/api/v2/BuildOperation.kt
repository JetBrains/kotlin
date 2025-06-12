/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.Mandatory
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.WithDefault
import org.jetbrains.kotlin.buildtools.api.v2.trackers.BuildMetricsCollector

public interface BuildOperation<R> {
    public interface Option<V>

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        private fun <V> mandatory(id: String): Option<V> =
            object : Mandatory(id), Option<V> {}

        private fun <V> optional(id: String, defaultValue: V): Option<V> =
            object : WithDefault<V>(id, defaultValue), Option<V> {}

        // TODO: opt-in that marks it as requiring explicit cleanup by `finishBuild`
        /**
         * Marks build operation as scoped to a project build. Allows tools to avoid dropping some caches.
         */
        @JvmField
        public val PROJECT_ID: Option<ProjectId?> = optional("PROJECT_ID", null)

        @JvmField
        public val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = optional("METRICS_COLLECTOR", null)
    }
}