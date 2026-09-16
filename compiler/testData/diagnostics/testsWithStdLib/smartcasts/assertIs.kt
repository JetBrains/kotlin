// RUN_PIPELINE_TILL: CODEGEN
import kotlin.test.*

fun foo(arg: Any) {
    assertIs<String>(arg, "")
    arg.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, smartcast, stringLiteral */
