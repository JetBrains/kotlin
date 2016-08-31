fun f() {
    var foo = 1
    try {
        foo = 2 
        throw RuntimeException()
    } catch (e: Throwable) {
        println(foo)
    }
}
