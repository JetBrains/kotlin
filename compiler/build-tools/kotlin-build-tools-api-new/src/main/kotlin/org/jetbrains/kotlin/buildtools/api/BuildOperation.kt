/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

public interface BuildOperation<R> {
    public class Option<V> internal constructor(public val id: String)

    public operator fun <V> get(key: Option<V>): V?

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        // TODO: opt-in that marks it as requiring explicit cleanup by `finishBuild`
        /**
         * Marks build operation as scoped to a project build. Allows tools to avoid dropping some caches.
         */
        @JvmField
        public val PROJECT_ID: Option<ProjectId> = Option("PROJECT_ID")

        @JvmField
        public val METRICS_COLLECTOR: Option<BuildMetricsCollector> = Option("METRICS_COLLECTOR")
    }
}