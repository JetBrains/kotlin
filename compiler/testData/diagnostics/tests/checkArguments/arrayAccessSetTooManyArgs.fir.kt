// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    operator fun get(x: Int) {}
    operator fun set(x: String, value: Int) {}

    fun d(x: Int) {
        <!INAPPLICABLE_CANDIDATE!>this["", 1] = 1<!>
    }
}
