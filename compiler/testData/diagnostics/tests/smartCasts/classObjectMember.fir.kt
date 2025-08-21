// RUN_PIPELINE_TILL: FRONTEND
open class T {
    val x : Int? = null
}

class A {
    companion object: T() {
    }
}

class B {
    companion object: T() {
    }
}

fun test() {
    if (A.x != null) {
        useInt(A.x)
        useInt(<!ARGUMENT_TYPE_MISMATCH!>B.x<!>)
    }
}

fun useInt(i: Int) = i

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, ifExpression,
nullableType, objectDeclaration, propertyDeclaration, smartcast */
