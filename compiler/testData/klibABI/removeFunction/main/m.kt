
fun test1(): String {
    try {
        return qux(true)
    } catch(ex: Throwable) {
        return "O"
    }

    return "FAIL2"
}

fun test2(): String = qux(false)

fun box(): String {
    return test1() + test2()
}