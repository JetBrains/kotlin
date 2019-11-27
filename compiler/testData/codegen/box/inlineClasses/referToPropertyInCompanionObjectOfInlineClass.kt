// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Foo(val c: Char) {
    companion object {
        val prop = "O"
        const val constVal = 1
        fun funInCompanion(): String = "K"
    }

    fun simple() {
        prop
        constVal
        funInCompanion()
    }

    fun asResult(): String = prop + constVal + funInCompanion() + c
}

fun box(): String {
    val r = Foo('2')
    if (r.asResult() != "O1K2") return "fail"
    return "OK"
}