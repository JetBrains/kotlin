class Test {
    var storage = "Fail"

    var Int.foo: String
        get() = storage
        set(value) {
            storage = value
        }

    fun test(): String {
        val i = 1
        i.foo = "OK"
        return i.foo
    }
}

fun box(): String {
    return Test().test()
}
