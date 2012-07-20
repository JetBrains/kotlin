fun foo(i: Int) : Int =
    when (i) {
        1 -> 1
        null -> 1
        else -> 1
    }

fun box() : String = if (foo(1) == 1) "OK" else "fail"
