// WITH_STDLIB

class TestException(message: String) : Exception(message)

fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { if (it == 2) throw TestException("boom:$it") else it }

    try {
        val result = mutableListOf<Int>()
        for (x in seq) {
            result.add(x)
        }
        return "fail: exception was not thrown, result=$result"
    } catch (e: TestException) {
        return if (e.message != "boom:2") {
            "fail: wrong exception message: ${e.message}"
        } else "OK"
    }
}