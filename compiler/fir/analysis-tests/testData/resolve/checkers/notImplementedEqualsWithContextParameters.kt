// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

interface I {
    context(_: Int)
    fun equals(x: Any): Boolean
}

interface I2 {
    fun Int.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(x: Any): Boolean
}

data <!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'C'; members:context(<unused var>: Int) fun equals(x: Any): Booleanfun Int.equals(x: Any): Boolean")!>class C<!>(val x: String) : I, I2
