// RUN_PIPELINE_TILL: BACKEND
object Foo {
    operator fun <T> invoke() {}
}

fun main() {
    Foo<Int>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, objectDeclaration, operator, typeParameter */
