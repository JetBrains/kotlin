/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.light.classes.symbol.KaInternalsLightClassBridge

context(session: KaSession)
internal val lightClassBridge: KaInternalsLightClassBridge
    get() = KotlinAsJavaSupport.getInstance(session.useSiteModule.project) as KaInternalsLightClassBridge
