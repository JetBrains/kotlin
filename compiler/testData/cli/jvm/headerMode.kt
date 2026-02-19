class Owner {
    val x: Double = y // In header mode we shouldn't have an error here
        get() = 42    // Again, no error in header mode

    fun foo(): String {
        doSomething() // In header mode we shouldn't have an error here
        return "OK"
    }
}

fun bar(): Int {
    return Owner().foo().length
}
