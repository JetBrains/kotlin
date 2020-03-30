class B

fun B.magic() {
}

fun boom(a: Any) {
    when (a) {
        is B -> run(a::magic)
    }
}

fun box(): String {
    boom(B())
    return "OK"
}
