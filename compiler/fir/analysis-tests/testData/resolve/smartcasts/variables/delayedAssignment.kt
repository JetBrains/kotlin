// RUN_PIPELINE_TILL: FRONTEND
// DUMP_CFG
class A {
    fun foo() {}
}

fun test(b: Boolean) {
    val a: A?
    if (b) {
        a = A()
        a.foo()
    } else {
        a = null
    }
    a<!UNSAFE_CALL!>.<!>foo()
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast */
