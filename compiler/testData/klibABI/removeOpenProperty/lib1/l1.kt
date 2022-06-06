open class C {
    open val foo: String get() = "FAIL1"
}

interface I {
    val foo: String get() = "FAIL2"
}
