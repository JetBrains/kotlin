// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C {
    val a: Any
        field: Int = 1

    private inline fun privateFun() = a.inc()
    public inline fun publicFun() = a.inc()

    public inline fun outer() {
        val local = object {
            val b: Any
                field: Int = 1
            private inline fun privateInner() {
                <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>a<!>.inc()
                b.inc()
            }
        }
        a.inc()
        local.b.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, explicitBackingField, functionDeclaration, inline,
integerLiteral, localProperty, propertyDeclaration, smartcast */
