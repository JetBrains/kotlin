// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// LATEST_LV_DIFFERENCE

// MODULE: m1
// FILE: f1.kt
package p1

enum class SimpleE {
    @Deprecated("", level = HIDDEN)
    Hidden,
    @Deprecated("", level = ERROR)
    Error,
    @Deprecated("", level = WARNING)
    Warning,
    Simple,

    @Deprecated("", level = HIDDEN)
    HiddenWithMethod {
        fun method() { }
    },
    @Deprecated("", level = ERROR)
    ErrorWithMethod {
        fun method() { }
    },
    @Deprecated("", level = WARNING)
    WarningWithMethod {
        fun method() { }
    },
    SimpleWithMethod {
        fun method() { }
    }
}

enum class ConstructorE(val x: String) {
    @Deprecated("", level = HIDDEN)
    Hidden("h"),
    @Deprecated("", level = ERROR)
    Error("e"),
    @Deprecated("", level = WARNING)
    Warning("w"),
    Simple("s"),
}

// MODULE: m2(m1)
// FILE: f2.kt
package p2

import p1.*

fun useSites() {
    val _: SimpleE = <!UNRESOLVED_REFERENCE!>Hidden<!>
    val _: SimpleE = <!DEPRECATION_ERROR!>Error<!>
    val _: SimpleE = <!DEPRECATION!>Warning<!>
    val _: SimpleE = Simple

    val _: SimpleE = <!UNRESOLVED_REFERENCE!>HiddenWithMethod<!>
    val _: SimpleE = <!DEPRECATION_ERROR!>ErrorWithMethod<!>
    val _: SimpleE = <!DEPRECATION!>WarningWithMethod<!>
    val _: SimpleE = SimpleWithMethod

    val _: ConstructorE = <!UNRESOLVED_REFERENCE!>Hidden<!>
    val _: ConstructorE = <!DEPRECATION_ERROR!>Error<!>
    val _: ConstructorE = <!DEPRECATION!>Warning<!>
    val _: ConstructorE = Simple
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, unnamedLocalVariable */
