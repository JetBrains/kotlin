/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * This event signals that project settings or project structure are changing possibly globally.
 *
 * The module structure, source code, and binary content of all [KaModule]s in the project should be considered modified when this event is
 * received. This includes source files being moved or removed, binary content being added, removed, or changed, and modules possibly being
 * removed. Thus, all caches related to module structure, source code, and binaries should be invalidated.
 *
 * See [KotlinModificationEvent] for important contracts common to all modification events.
 */
public object KotlinGlobalModuleStateModificationEvent : KotlinModificationEvent
