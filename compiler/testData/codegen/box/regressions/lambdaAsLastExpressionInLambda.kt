val foo: ((String) -> String) = run {
    { it }
}

fun box() = foo("OK")