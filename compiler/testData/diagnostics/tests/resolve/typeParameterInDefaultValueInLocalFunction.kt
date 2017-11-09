// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    fun <T> bar(x: List<T> = listOf<T>()) {}
}

fun <T> listOf(): List<T> = TODO()