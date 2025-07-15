// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

interface I {
    fun foo(): String
}

class X(val i: I): I by i

class Y(val i: I): I by i {
    override fun foo(): String = ""
}

fun test(x: X, y: Y) {
    <!RETURN_VALUE_NOT_USED!>x.foo()<!>
    <!RETURN_VALUE_NOT_USED!>y.foo()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, override,
primaryConstructor, propertyDeclaration */
