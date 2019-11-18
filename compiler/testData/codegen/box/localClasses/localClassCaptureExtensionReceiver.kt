// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    val foo = "Foo"

    fun String.id(): String {
        class Local(unused: Long) {
            fun result() = this@id
            fun outer() = this@Outer
        }

        val l = Local(42L)
        return l.result() + l.outer().foo
    }

    fun result(): String = "OK".id()
}

fun box(): String {
    val r = Outer().result()

    if (r != "OKFoo") return "Fail: $r"

    return "OK"
}
