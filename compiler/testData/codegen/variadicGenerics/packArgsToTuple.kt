// !LANGUAGE: +NewInference

fun box(): String {
    wrapArgsInTuple()
    wrapArgsInTuple(1)
    wrapArgsInTuple(1, "2")
    return "OK"
}

fun <vararg Ts> wrapArgsInTuple (
    vararg args: *Ts
) {}
