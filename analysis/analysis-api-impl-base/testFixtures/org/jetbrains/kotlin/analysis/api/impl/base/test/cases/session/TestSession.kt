/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule

/**
 * A test wrapper around an LL FIR or analysis session.
 *
 * @param S The type of the underlying session.
 */
abstract class TestSession<S> {
    abstract val underlyingSession: S

    abstract val ktTestModule: KtTestModule

    abstract val isValid: Boolean

    /**
     * A human-readable description which identifies the *session* (not its module).
     */
    abstract val description: String

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestSession<*>) return false
        return underlyingSession == other.underlyingSession
    }

    override fun hashCode(): Int = underlyingSession.hashCode()
}
