// RUN_PIPELINE_TILL: CODEGEN
object Foo {
    operator fun <T> invoke() {}
}

fun main() {
    Foo<Int>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, objectDeclaration, operator, typeParameter */
