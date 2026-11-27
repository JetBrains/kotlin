// RUN_PIPELINE_TILL: CODEGEN
// LATEST_LV_DIFFERENCE

// MODULE: m1
// FILE: f1.kt
package p1

enum class SimpleE {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    Hidden,
    @Deprecated("", level = DeprecationLevel.ERROR)
    Error,
    @Deprecated("", level = DeprecationLevel.WARNING)
    Warning,
    Simple,

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    HiddenWithMethod {
        fun method() { }
    },
    @Deprecated("", level = DeprecationLevel.ERROR)
    ErrorWithMethod {
        fun method() { }
    },
    @Deprecated("", level = DeprecationLevel.WARNING)
    WarningWithMethod {
        fun method() { }
    },
    SimpleWithMethod {
        fun method() { }
    }
}

enum class ConstructorE(val x: String) {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    Hidden("h"),
    @Deprecated("", level = DeprecationLevel.ERROR)
    Error("e"),
    @Deprecated("", level = DeprecationLevel.WARNING)
    Warning("w"),
    Simple("s"),
}

// MODULE: m2(m1)
// FILE: f2.kt
package p2

import p1.*

fun useSites() {
    val _ = SimpleE.Hidden
    val _ = SimpleE.Error
    val _ = SimpleE.Warning
    val _ = SimpleE.Simple

    val _ = SimpleE.HiddenWithMethod
    val _ = SimpleE.ErrorWithMethod
    val _ = SimpleE.WarningWithMethod
    val _ = SimpleE.SimpleWithMethod

    val _ = ConstructorE.Hidden
    val _ = ConstructorE.Error
    val _ = ConstructorE.Warning
    val _ = ConstructorE.Simple
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, unnamedLocalVariable */
