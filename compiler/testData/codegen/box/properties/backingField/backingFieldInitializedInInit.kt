// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB

class Test {
    val numbers: List<Int> field: MutableList<Int>

    init {
        numbers = mutableListOf(1, 2, 3)
    }
}

fun box() = when {
    Test().numbers == listOf(1, 2, 3) -> "OK"
    else -> "Fail: " + Test().numbers
}
