// IGNORE_BACKEND_FIR: JVM_IR
fun String.foo() : String {
    fun Int.bar() : String {
        fun Long.baz() : String {
            val x = this@foo
            val y = this@bar
            val z = this@baz
            return "$x $y $z"
        }
        return 0L.baz()
    }
    return 42.bar()
}

fun box() : String {
    val result = "OK".foo()

    if (result != "OK 42 0") return "fail: $result"

    return "OK"
}