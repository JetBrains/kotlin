// IGNORE_BACKEND_FIR: JVM_IR
var fooStorage = "Fail"
var barStorage = "Fail"

var Double.foo: String
    get() = fooStorage
    set(value) {
        fooStorage = value
    }

var Long.bar: String
    get() = barStorage
    set(value) {
        barStorage = value
    }

fun box(): String {
    val d = 1.0
    d.foo = "O"
    val l = 1L
    l.bar = "K"
    return d.foo + l.bar
}
