// WITH_STDLIB

fun box(): String {
    listOf(1).forEach { size ->
        repeat(size) {
            return "OK"
        }
    }
    return "Fail"
}
