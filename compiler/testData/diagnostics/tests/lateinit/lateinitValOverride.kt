// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

// MODULE: lib

open class Base {
    open lateinit val foo: String
    open lateinit val bar: String
}

// MODULE: main(lib)

class Derived : Base() {
    override lateinit val foo: String
    override <!LATEINIT_VAL_OVERRIDDEN_BY_VAL!>val<!> bar: String = ""
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
