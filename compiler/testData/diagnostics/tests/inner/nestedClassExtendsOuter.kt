// RUN_PIPELINE_TILL: BACKEND
open class Outer {
    class Nested : Outer() {
        fun bar() = foo()
        fun baz() = super.foo()
    }
    
    fun foo() = 42
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nestedClass, superExpression */
