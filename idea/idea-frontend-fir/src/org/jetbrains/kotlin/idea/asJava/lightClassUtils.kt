/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt

@OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
internal inline fun <E> allowLightClassesOnEdt(action: () -> E): E = hackyAllowRunningOnEdt(action)

internal inline fun <T> Boolean.ifTrue(body: () -> T?): T? = if (this) body() else null

internal inline fun <T> Boolean.ifFalse(body: () -> T?): T? = if (!this) body() else null