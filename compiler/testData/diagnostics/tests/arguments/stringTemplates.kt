// RUN_PIPELINE_TILL: CODEGEN
class A

fun foo(s: String) {}

fun test(a: A) {
    foo("$a")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
