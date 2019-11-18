// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class UInt(val a: Int) {
    fun test() {
        takeNullable(this)
        takeAnyInside(this)

        takeAnyInside(this.a)
    }

    fun takeAnyInside(a: Any) {}
}

fun takeNullable(a: UInt?) {}

fun box(): String {
    val a = UInt(0)
    a.test()

    return "OK"
}