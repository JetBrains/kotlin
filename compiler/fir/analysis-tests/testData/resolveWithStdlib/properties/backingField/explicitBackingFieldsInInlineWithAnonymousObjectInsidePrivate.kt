// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

val a: Any
    field: Int = 1
private inline fun foo() {
    val local = object {
        val b: Any
            field: Int = 1
        public inline fun inner() {
            a.inc()
            b.inc()
        }
    }
}

fun bar() {
    val local = object {
        val b: Any
            field: Int = 1
        public inline fun inner() {
            a.inc()
            b.inc()
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, explicitBackingField, functionDeclaration, inline, integerLiteral,
localProperty, propertyDeclaration, smartcast */
