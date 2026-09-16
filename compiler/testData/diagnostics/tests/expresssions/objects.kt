// RUN_PIPELINE_TILL: CODEGEN
object A {
    fun foo() = this
}

fun use() = A
fun bar() = A.foo()

/* GENERATED_FIR_TAGS: functionDeclaration, objectDeclaration, thisExpression */
