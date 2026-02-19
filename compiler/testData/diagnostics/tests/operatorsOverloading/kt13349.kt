// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object Foo {
    operator fun <T> invoke() {}
}

fun main() {
    Foo<Int>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, objectDeclaration, operator, typeParameter */
