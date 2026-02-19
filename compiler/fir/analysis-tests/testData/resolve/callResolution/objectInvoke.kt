// RUN_PIPELINE_TILL: BACKEND
object Bar {
    operator fun invoke(x: String) {}
}

fun foo() {
    Bar("asd")
}

/* GENERATED_FIR_TAGS: functionDeclaration, objectDeclaration, operator, stringLiteral */
