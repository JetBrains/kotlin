// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// LANGUAGE: +ExplicitBackingFields
// ^This explicit setting is only needed for some Native runners,
//  as they haven't been translated to our proper test infrastructure.

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
