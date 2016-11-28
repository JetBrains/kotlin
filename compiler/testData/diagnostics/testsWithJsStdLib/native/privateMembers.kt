// !DIAGNOSTICS: -NOTHING_TO_INLINE
// TODO: should we disable NOTHING_TO_INLINE in JS backend?

external class C {
    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private fun a(): Int<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private val b: String<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var c: Float<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var d: Float<!>
        get
        set

    private inline fun inline_a(): Int = 23

    private inline val inline_prop: Int
        get() = 42
}

external object O {
    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private fun a(): Int<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private val b: String<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var c: Float<!>

    <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var d: Float<!>
        get
        set

    private inline fun inline_a(): Int = 23

    private inline val inline_prop: Int
        get() = 42
}

external class Outer {
    class Inner {
        <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private fun a(): Int<!>

        <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private val b: String<!>

        <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var c: Float<!>

        <!EXTERNAL_CLASS_PRIVATE_MEMBER!>private var d: Float<!>
            get
            set

        private inline fun inline_a(): Int = 23

        private inline val inline_prop: Int
            get() = 42
    }
}