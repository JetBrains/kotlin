/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.statistics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry

/**
 * Provides the global [OpenTelemetry] instance as the platform's instance. This approach requires the platform to initialize the
 * OpenTelemetry SDK as a global instance.
 */
public class KotlinGlobalOpenTelemetryProvider : KotlinOpenTelemetryProvider {
    override val openTelemetry: OpenTelemetry
        get() = GlobalOpenTelemetry.get()
}
