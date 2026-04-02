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
    x.<!RETURN_VALUE_NOT_USED!>foo<!>()
    y.<!RETURN_VALUE_NOT_USED!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, override,
primaryConstructor, propertyDeclaration */
