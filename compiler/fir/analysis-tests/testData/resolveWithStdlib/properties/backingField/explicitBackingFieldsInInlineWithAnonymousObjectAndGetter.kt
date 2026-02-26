// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

val a: Any
    field: Int = 1

inline fun outer() {
    val local = object {
        val b: Any
            field: Int = 1
        private val c: Int
            inline get() {
                a.<!UNRESOLVED_REFERENCE!>inc<!>()
                b.<!UNRESOLVED_REFERENCE!>inc<!>()
                return 1
            }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, explicitBackingField, functionDeclaration, getter, inline,
integerLiteral, localProperty, propertyDeclaration */
