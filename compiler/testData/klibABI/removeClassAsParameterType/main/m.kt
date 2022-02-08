
fun test1(d: D): String {
    try {
        d.bar()
    } catch(ex: Throwable) {
        return "O"
    }

    return "FAIL2"
}

fun test2(d: D): String {
    return d.foo()
}


fun box(): String {
    return test1(D()) + test2(D())
}