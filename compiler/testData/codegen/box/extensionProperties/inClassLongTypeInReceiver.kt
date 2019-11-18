// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    var doubleStorage = "fail"
    var longStorage = "fail"

    var Double.foo: String
        get() = doubleStorage
        set(value) {
            doubleStorage = value
        }

    var Long.bar: String
        get() = longStorage
        set(value) {
            longStorage = value
        }

    fun test(): String {
        val d = 1.0
        d.foo = "O"
        val l = 1L
        l.bar = "K"
        return d.foo + l.bar
    }
}

fun box(): String {
    return Test().test()
}
