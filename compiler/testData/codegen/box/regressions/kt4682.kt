fun foo(compareTo: Any.(p: Function0<Int>) -> Int, p: () -> Int) {
    p < p
}

fun box(): String {
    foo({ it() }, { 42 })
    return "OK"
}
