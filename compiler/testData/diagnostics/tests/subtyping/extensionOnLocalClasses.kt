// RUN_PIPELINE_TILL: BACKEND
package foo.bar

fun test() {
    class A {
        inner class B
    }

    fun A.B.foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, inner, localClass, localFunction */
