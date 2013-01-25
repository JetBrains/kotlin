fun foo(i: Int, j: Int?): String =
    when (i) {
        j -> "OK"
        else -> "Fail"
    }

fun box(): String = foo(0, 0)
