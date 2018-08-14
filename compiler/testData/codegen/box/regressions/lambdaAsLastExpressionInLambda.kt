// IGNORE_BACKEND: JS_IR
val foo: ((String) -> String) = run {
    { it }
}

fun box() = foo("OK")