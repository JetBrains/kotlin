// RUN_PIPELINE_TILL: CODEGEN
// KT-3731 Resolve & inner class

class A {
    fun foo() {}
    fun bar(f: A.() -> Unit = {}) = f()
}

class B {
    class D {
        init {
            A().bar {
                this.foo()
                foo()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, init, lambdaLiteral, nestedClass,
thisExpression, typeWithExtension */
