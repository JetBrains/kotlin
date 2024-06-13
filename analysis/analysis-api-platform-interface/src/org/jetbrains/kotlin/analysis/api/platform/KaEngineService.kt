/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

/**
 * An **engine service** as defined by the Platform Interface (see the README).
 *
 * Engine services do not need to be implemented by a platform. Quite the contrary, they are implemented by the Analysis API engine and
 * intended to support platform implementations. They are defined in the Platform Interface, as opposed to the user-facing Analysis API,
 * because they are intended for the consumption of platform implementations, but not Analysis API users.
 *
 * As an example, a platform's lifetime token implementation (see [KotlinLifetimeTokenProvider][org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenProvider])
 * can make use of the [KaLifetimeTracker][org.jetbrains.kotlin.analysis.api.platform.lifetime.KaLifetimeTracker] engine service to retrieve
 * the currently active lifetime token for comparison.
 *
 * As a marker interface, [KaEngineService] clearly separates an engine service from [KotlinPlatformComponent]s which need to be implemented
 * by a platform. Furthermore, engine services are always prefixed with `Ka`, in contrast to platform components which are prefixed with
 * `Kotlin`.
 */
public interface KaEngineService
