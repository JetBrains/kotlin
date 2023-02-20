/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.util.ModificationTracker
import java.lang.ref.WeakReference

class FirSessionValidityStamp(session: LLFirSession) : ModificationTracker {
    private val sessionRef = WeakReference(session)

    val isValid: Boolean
        get() = modificationCount == 0L

    override fun getModificationCount(): Long {
        val session = sessionRef.get() ?: return 1
        return if (session.isValid) 0 else 2
    }
}