/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform

/**
 * A **platform component** as defined by the Platform Interface (see the README).
 *
 * Mandatory platform components must be implemented by a platform to fully support the Analysis API in the desired environment. A few
 * platform components are optional, signified by [KotlinOptionalPlatformComponent]. As a marker interface, [KotlinPlatformComponent]
 * makes it easy to find all platform components to implement.
 *
 * The Platform Interface provides a number of default and base implementations which may be specified or extended by a platform
 * implementation, such as [KotlinProjectMessageBusProvider] for [KotlinMessageBusProvider].
 *
 * Platform component interfaces and their default implementations are always prefixed with the word `Kotlin`, in contrast to
 * [KaEngineService]s which are prefixed with `Ka`. It is recommended to keep this naming convention in platform implementations. For
 * example, the Standalone API uses a `KotlinStandalone` prefix for its own platform component implementations.
 */
public interface KotlinPlatformComponent

/**
 * An optional [KotlinPlatformComponent]. The Analysis API engine does not require an optional platform component to be implemented and
 * will use sensible fallbacks or disable/avoid certain behaviors instead.
 */
public interface KotlinOptionalPlatformComponent : KotlinPlatformComponent
