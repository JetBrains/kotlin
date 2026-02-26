// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
val a: Any
    field: Int = 1

inline fun outer(vararg x: Int = intArrayOf(a.<!UNRESOLVED_REFERENCE!>inc<!>())) {
    val local = object {
        val b: Any
            field: Int = 1

        private inline fun inner(vararg y: Int = intArrayOf(b.<!UNRESOLVED_REFERENCE!>inc<!>(), a.<!UNRESOLVED_REFERENCE!>inc<!>())) {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, explicitBackingField, functionDeclaration, inline, integerLiteral,
localProperty, propertyDeclaration, vararg */
