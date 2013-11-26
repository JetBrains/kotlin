tailRecursive fun withWhen(counter : Int, d : Any, x : Any) : Int =
    if (counter == 0) {
        0
    }
    else if (counter == 5) {
        withWhen(counter - 1, 999, "tail")
    }
    else
        when (d) {
            is String -> withWhen(counter - 1, "is String", "tail")
            is Number -> withWhen(counter, "is Number", "tail")
            else -> throw IllegalStateException()
        }

fun box() : String = if (withWhen(100000, "test", "test") == 0) "OK" else "FAIL"
