// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class IC(val x: String) {
    private fun privateFun() = x
    override fun toString() = privateFun()
}

fun box(): String {
    val x: Any = IC("OK")
    return x.toString()
}