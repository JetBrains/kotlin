// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -OVERRIDE_BY_INLINE

val a: Any
    field: Int = 1

open class A {
    internal open fun foo() {}
    open fun bar() {}
}

class B: A() {
    val b: Any
        field: Int = 1
    override inline fun foo() {
        a.inc()
        b.inc()
    }
    override inline fun bar() {
        a.<!UNRESOLVED_REFERENCE!>inc<!>()
        b.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, inline, integerLiteral, override,
propertyDeclaration, smartcast */
