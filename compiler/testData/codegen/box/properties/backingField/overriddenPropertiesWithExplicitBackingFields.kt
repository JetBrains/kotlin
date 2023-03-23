// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

open class A {
    open var it: Number
        private field = 3
        set(value) {
            field = value.toInt()
        }

    fun test(): String {
        // Note that `it` is open,
        // so no smart type narrowing
        // is possible, and we expect
        // here a call to the possibly
        // overridden getter
        return if (it.toInt() + 1 == 11) {
            "OK"
        } else {
            "fail: $it"
        }
    }
}

open class B : A() {
    override var it: Number
        get() = 10.12
        set(value) {}
}

fun box(): String {
    return B().test()
}
