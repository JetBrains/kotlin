/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule

/**
 * This event signals that Kotlin script module settings or structure are changing possibly globally.
 *
 * When this event is received, the module structure, source code, and binary content of all [KaScriptModule]s and
 * [KaScriptDependencyModule]s in the project should be considered modified. Therefore, all caches related to Kotlin script module
 * structure, source code, and binaries must be invalidated.
 *
 * See [KotlinModificationEvent] for important contracts common to all modification events.
 */
@KaPlatformInterface
public object KotlinGlobalScriptModuleStateModificationEvent : KotlinModificationEvent
