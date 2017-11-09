package test

fun test(f: () -> Unit) {
    try {
        f()
    }
    catch (e: MyException) {
        f()
    }
}
