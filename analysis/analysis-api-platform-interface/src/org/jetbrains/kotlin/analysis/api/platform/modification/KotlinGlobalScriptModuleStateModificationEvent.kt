/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule

/**
 * [KotlinGlobalScriptModuleStateModificationEvent] signals that Kotlin script module settings or structure are changing possibly globally.
 * The event is published in a write action before or after the modification.
 *
 * When this event is received, the module structure, source code, and binary content of all [KaScriptModule]s and
 * [KaScriptDependencyModule]s in the project should be considered modified. Therefore, all caches related to Kotlin script module
 * structure, source code, and binaries must be invalidated.
 */
public object KotlinGlobalScriptModuleStateModificationEvent : KotlinModificationEvent
