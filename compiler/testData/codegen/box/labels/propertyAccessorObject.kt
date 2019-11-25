// IGNORE_BACKEND_FIR: JVM_IR
interface Base {
    fun foo(): Int
}

val Int.getter: Int
    get() {
        return object : Base {
            override fun foo(): Int {
                return this@getter
            }
        }.foo()
    }

fun box(): String {
    val i = 1
    if (i.getter != 1) return "getter failed"

    return "OK"
}
