// RUNTIME_WITH_FULL_JDK

enum class E {
    A, B
}

fun main() {
    val test: Map<E, String> = <caret>HashMap()
}
