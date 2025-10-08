// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

class MyClass {
    companion object {
        val X: MyClass = MyClass()
        val Y: String = ""
    }
}

val x: MyClass = X
val y: MyClass <!INITIALIZER_TYPE_MISMATCH!>=<!> Y

fun myX(): MyClass = X
fun myY(): MyClass = <!RETURN_TYPE_MISMATCH!>Y<!>

fun foo(m: MyClass = X) {}

val property1: MyClass
    get() = X

val property2
    get(): MyClass = X

val property3: MyClass
    get() = <!RETURN_TYPE_MISMATCH!>Y<!>

val property4
    get(): MyClass = <!RETURN_TYPE_MISMATCH!>Y<!>

fun main() {
    var m: MyClass? = null
    m = <!ASSIGNMENT_TYPE_MISMATCH!>Y<!>
    m = X
}

fun bar(b: Boolean): MyClass {
    if (b) return X
    return <!RETURN_TYPE_MISMATCH!>Y<!>
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, companionObject, functionDeclaration, getter, ifExpression,
localProperty, nullableType, objectDeclaration, propertyDeclaration, stringLiteral */
