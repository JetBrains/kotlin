fun foo() {
    "before"
    try {
        foo()
    }
    catch (e: Exception) {
        val a = e
    }
    finally {
        val a = 1
    }
    "after"
}