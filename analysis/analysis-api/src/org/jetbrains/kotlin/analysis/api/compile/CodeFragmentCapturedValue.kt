/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compile

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

public sealed class CodeFragmentCapturedValue(
    public val name: String,
    public val isMutated: Boolean,
    public val isCrossingInlineBounds: Boolean,
) {
    public open val displayText: String
        get() = name

    override fun toString(): String {
        return this::class.simpleName + "[name: " + name + "; isMutated: " + isMutated + "; displayText: " + displayText + "]"
    }

    /** Represents a local variable or a parameter. */
    public class Local(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds)

    /** Represents a delegated local variable (`val local by...`). */
    public class LocalDelegate(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds) {
        override val displayText: String
            get() = "$name\$delegate"
    }

    /** Represents a backing field (a `field` variable inside a property accessor). */
    public class BackingField(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds) {
        override val displayText: String
            get() = "field"
    }

    /** Represents a captured outer class. */
    public class ContainingClass(
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
    public class SuperClass(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue("<super>", isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "super@" + classId.shortClassName.asString()
    }

    public class ExtensionReceiver(
        labelName: String,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(labelName, isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "this@$name"
    }

    public class ContextReceiver(
        public val index: Int,
        labelName: Name,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(labelName.asString(), isMutated = false, isCrossingInlineBounds) {
        override val displayText: String
            get() = "this@$name"
    }

    /** Represents an externally provided value. */
    public class ForeignValue(
        name: Name,
        isCrossingInlineBounds: Boolean,
    ) : CodeFragmentCapturedValue(name.asString(), isMutated = false, isCrossingInlineBounds)

    /** Represents a `coroutineContext` call. */
    public class CoroutineContext(
        isCrossingInlineBounds: Boolean
    ) : CodeFragmentCapturedValue("coroutineContext", isMutated = false, isCrossingInlineBounds)
}