tailrec fun foo() {
    try {
        return foo()
    }
    catch (e: Throwable) {
    }
}
