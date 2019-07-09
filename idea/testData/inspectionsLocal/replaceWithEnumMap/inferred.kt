// RUNTIME_WITH_FULL_JDK

enum class E {
    A, B
}

fun getMap(): Map<E, String> = <caret>hashMapOf()
