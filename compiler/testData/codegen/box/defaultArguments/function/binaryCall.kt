fun Int.foo(o: String, k: String = "K") = o + k

fun box() = 42 foo "O"
