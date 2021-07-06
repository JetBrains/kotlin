/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.tokens

import com.intellij.openapi.project.Project
import kotlin.reflect.KClass

public abstract class ValidityToken {
    public abstract fun isValid(): Boolean
    public abstract fun getInvalidationReason(): String

    public abstract fun isAccessible(): Boolean
    public abstract fun getInaccessibilityReason(): String
}

public abstract class ValidityTokenFactory {
    public abstract val identifier: KClass<out ValidityToken>
    public abstract fun create(project: Project): ValidityToken

    public open fun beforeEnteringAnalysisContext() {}
    public open fun afterLeavingAnalysisContext() {}
}


@Suppress("NOTHING_TO_INLINE")
public inline fun ValidityToken.assertIsValidAndAccessible() {
    if (!isValid()) {
        throw InvalidEntityAccessException("Access to invalid $this: ${getInvalidationReason()}")
    }
    if (!isAccessible()) {
        throw InaccessibleEntityAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
    }
}

public abstract class BadEntityAccessException() : IllegalStateException()

public class InvalidEntityAccessException(override val message: String) : BadEntityAccessException()
public class InaccessibleEntityAccessException(override val message: String) : BadEntityAccessException()

