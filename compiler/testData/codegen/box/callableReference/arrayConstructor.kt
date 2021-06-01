// WITH_RUNTIME

fun g(b: (Int, (Int) -> String) -> Array<String>): Array<String> =
    b(1) { "OK" }

fun box(): String = g(::Array)[0]
