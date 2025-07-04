// RUN_PIPELINE_TILL: FRONTEND
val x = ""

fun bar(x : Int = <!TYPE_MISMATCH!>""<!>, y : Int = x, z : String = <!TYPE_MISMATCH!>y<!>) {

}

// KT-371 Resolve default parameters for constructors

class A(x : Int = <!UNINITIALIZED_PARAMETER!>y<!>, y : Int = x) { // None of the references is resolved, no types checked
    constructor(x : Int = <!UNINITIALIZED_PARAMETER!>x<!>) : this(x, x)
    fun foo(bool: Boolean, a: Int = <!TYPE_MISMATCH, UNINITIALIZED_PARAMETER!>b<!>, b: String = <!TYPE_MISMATCH!>a<!>) {}
}

val z = 3

fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x, i : Int = z): Int = x + y

fun foo(x: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!> }, y: Int = x(), i : Int = z): Int = x() + y

fun bar(x: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!>; 1 }, y: Int) {}

fun baz(
    x: () -> Int = {
        fun bar(xx: () -> Int = { <!UNINITIALIZED_PARAMETER!>y<!>; 1 }) = xx
        bar()()
    },
    y: Int
) {
}

fun boo(
    x: () -> Int = {
        fun bar(): Int = <!UNINITIALIZED_PARAMETER!>y<!>
        bar()
    },
    y: Int
) {
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localFunction, primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral */
