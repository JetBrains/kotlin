// RUN_PIPELINE_TILL: FRONTEND
class A {
    constructor(x: Int = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>getSomeInt<!>(), other: A = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>, header: String = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>keker<!>) {}
    fun getSomeInt() = 10
    var keker = "test"
}

class B(other: B = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>)

class C() {
    constructor(x: Int) : this(<!ARGUMENT_TYPE_MISMATCH!>{
        val a = 10
        <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>
    }<!>) {}
}

class D {
    var a = 20
    constructor() {
        this.a = 10
    }
}

fun main() {
    val x1: String.() -> String = if (true) {
        { this }
    } else {
        { this }
    }
}

fun test(f: F) {}

val a = <!NO_THIS!>this<!>

class F(var a: Int, b: Int, closure: () -> Unit, instance: F?) {
    constructor(x: Int) : this(x, x, {
        val a = 10
        <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>
        test(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>)
        <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.a = 20
    }, <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>) {
        this.a = 30
    }
}

open class Base(val x: Int)

class Derived : Base(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.y) { // FE 1.0 reports NO_THIS here
    val y = 42
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral,
thisExpression, typeWithExtension */
