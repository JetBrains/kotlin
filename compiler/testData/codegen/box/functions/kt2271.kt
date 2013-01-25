fun foo(i: Int, j: Int = i) = j

fun box() = if (foo(1) == 1) "OK" else "fail"
