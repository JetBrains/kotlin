// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

val x: Any
    field: Int = 1

inline fun outer() {
    val local = object {
        val y: Any
            field: Int = 1

        private inline fun inner() {
            x.<!UNRESOLVED_REFERENCE!>inc<!>()
            y.<!UNRESOLVED_REFERENCE!>inc<!>()
        }
    }

    x.<!UNRESOLVED_REFERENCE!>inc<!>()
    local.y.<!UNRESOLVED_REFERENCE!>inc<!>()
}

inline fun outer2() {
    val local = object {
        val y: Any
            field: Int = 1

        inline fun publicInner() {
            x.<!UNRESOLVED_REFERENCE!>inc<!>()
            y.<!UNRESOLVED_REFERENCE!>inc<!>()
        }
    }

    x.<!UNRESOLVED_REFERENCE!>inc<!>()
    local.y.<!UNRESOLVED_REFERENCE!>inc<!>()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, explicitBackingField, functionDeclaration, inline, integerLiteral,
localProperty, propertyDeclaration, smartcast */
