fun <T> magic(): T = throw Exception()

fun <T> test(value: T, value2: T) {
    val x1: Any = value ?: 42
    val x2: Any = value ?: (value2 ?: 42)
    val x3: Any = (value ?: value2) ?: 42
    val x4: Any = value ?: value2 ?: 42
    val x5: Any = magic() ?: 42
    val x6: Any = value ?: magic() ?: 42
    val x7: Any = magic() ?: value ?: 42
}