// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    operator fun get(x: Int) {}
    operator fun set(x: String, value: Int) {}

    fun d(x: Int) {
        this["", <!TOO_MANY_ARGUMENTS{OI}!>1<!>] = <!TOO_MANY_ARGUMENTS{NI}!>1<!>
    }
}
