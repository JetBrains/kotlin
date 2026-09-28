// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK
fun foo(x: java.io.Serializable) {}

fun main() {
    foo("")
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
