// RUN_PIPELINE_TILL: FRONTEND

abstract class A {
    val a: String = "!"
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

object Obj : A() {
    val p: String = "!"
}

fun test(t: Any) {
    if (Obj == t) {
        t.a
        t.<!UNRESOLVED_REFERENCE!>p<!>
    }

    val local = object : A() {
        val p: String = "!"
    }
    if (local == t) {
        t.a
        t.<!UNRESOLVED_REFERENCE!>p<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, equalityExpression,
functionDeclaration, ifExpression, localProperty, nullableType, objectDeclaration, operator, override,
propertyDeclaration, smartcast, stringLiteral */
