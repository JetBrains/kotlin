/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

sealed class CodeFragmentCapturedValue(val name: String, val isMutated: Boolean, val isCrossingInlineBounds: Boolean) {
    open val displayText: String
        get() = name

    override fun toString(): String {
        return javaClass.simpleName + "[name: " + name + "; isMutated: " + isMutated + "; displayText: " + displayText + "]"
    }

    /** Represents a local variable or a parameter. */
    class Local internal constructor(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds)

    /** Represents a delegated local variable (`val local by...`). */
    class LocalDelegate internal constructor(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds) {
        override val displayText: String
            get() = "$name\$delegate"
    }

    /** Represents a backing field (a `field` variable inside a property accessor). */
    class BackingField internal constructor(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds) {
        override val displayText: String
            get() = "field"
    }

    /** Represents a captured outer class. */
    class ContainingClass internal constructor(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue("<this>", isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() {
                val simpleName = classId.shortClassName
                return if (simpleName.isSpecial) "this" else "this@" + simpleName.asString()
            }
    }

    /** Represents a captured super class (`super.foo()`). */
    class SuperClass internal constructor(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue("<super>", isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "super@" + classId.shortClassName.asString()
    }

    class ExtensionReceiver internal constructor(
        labelName: String,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(labelName, isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "this@$name"
    }

    class ContextReceiver internal constructor(
        val index: Int,
        labelName: Name,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(labelName.asString(), isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "this@$name"
    }

    /** Represents a captured named local function. */
    class LocalFunction internal constructor(
        name: Name,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated = false, isCrossingInlineBounds)

    /** Represents an externally provided value. */
    class ForeignValue internal constructor(
        name: Name,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated = false, isCrossingInlineBounds)

    /** Represents a `coroutineContext` call. */
    class CoroutineContext internal constructor(
        isCrossingInlineBounds: Boolean
    ) : CodeFragmentCapturedValue("coroutineContext", isMutated = false, isCrossingInlineBounds)
}