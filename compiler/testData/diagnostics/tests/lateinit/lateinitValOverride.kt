// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

// MODULE: lib

open class Base {
    open lateinit val foo: String
}

// MODULE: main(lib)

class Derived : Base() {
    override lateinit val foo: String
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
