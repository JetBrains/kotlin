// RUN_PIPELINE_TILL: CODEGEN
//KT-2142 function local classes do not work

package a

fun foo() {
    class Foo() {}
    Foo() // Unresolved reference Foo
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, primaryConstructor */
