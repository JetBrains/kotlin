/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compile

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@KaExperimentalApi
public sealed class CodeFragmentCapturedValue(
    public val name: String,
    public val isMutated: Boolean,
    public val isCrossingInlineBounds: Boolean,
    public val depthRelativeToCurrentFrame: Int
) {
    public open val displayText: String
        get() = name

    override fun toString(): String {
        return this::class.simpleName + "[name: " + name + "; isMutated: " + isMutated + "; displayText: " + displayText + "]"
    }

    /** Represents a local variable or a parameter. */
    @KaExperimentalApi
    public class Local(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds, depthRelativeToCurrentFrame)

    /** Represents a delegated local variable (`val local by...`). */
    @KaExperimentalApi
    public class LocalDelegate(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "$name\$delegate"
    }

    /** Represents a backing field (a `field` variable inside a property accessor). */
    @KaExperimentalApi
    public class BackingField(
        name: Name,
        isMutated: Boolean,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(name.asString(), isMutated, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "field"
    }

    /** Represents a captured outer class. */
    @KaExperimentalApi
    public class ContainingClass(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue("<this>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() {
                val simpleName = classId.shortClassName
                return if (simpleName.isSpecial) "this" else "this@" + simpleName.asString()
            }
    }

    /** Represents a captured super class (`super.foo()`). */
    @KaExperimentalApi
    public class SuperClass(
        private val classId: ClassId,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue("<super>", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "super@" + classId.shortClassName.asString()
    }

    @KaExperimentalApi
    public class ExtensionReceiver(
        labelName: String,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(labelName, isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "this@$name"
    }

    @KaExperimentalApi
    public class ContextReceiver(
        public val index: Int,
        labelName: Name,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(labelName.asString(), isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame) {
        override val displayText: String
            get() = "this@$name"
    }

    /** Represents an externally provided value. */
    @KaExperimentalApi
    public class ForeignValue(
        name: Name,
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue(name.asString(), isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame)

    /** Represents a `coroutineContext` call. */
    @KaExperimentalApi
    public class CoroutineContext(
        isCrossingInlineBounds: Boolean,
        depthRelativeToCurrentFrame: Int
    ) : CodeFragmentCapturedValue("coroutineContext", isMutated = false, isCrossingInlineBounds, depthRelativeToCurrentFrame)
}