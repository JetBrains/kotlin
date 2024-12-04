// RUN_PIPELINE_TILL: BACKEND
object Bar {
    operator fun invoke(x: String) {}
}

fun foo() {
    Bar("asd")
}
