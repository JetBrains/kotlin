// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec fun withWhen(counter : Int, d : Any) : Int =
    if (counter == 0) {
        0
    }
    else if (counter == 5) {
        withWhen(counter - 1, 999)
    }
    else
        when (d) {
            is String -> withWhen(counter - 1, "is String")
            is Number -> withWhen(counter, "is Number")
            else -> throw IllegalStateException()
        }

fun box() : String = if (withWhen(100000, "test") == 0) "OK" else "FAIL"
