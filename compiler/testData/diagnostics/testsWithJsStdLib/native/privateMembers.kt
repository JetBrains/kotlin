// FIR_IDENTICAL
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// TODO: should we disable NOTHING_TO_INLINE in JS backend?
// TODO: uncomment declarations in case we decide to implement KT-14031

external class C {
    <!WRONG_EXTERNAL_DECLARATION!>private fun a(): Int<!>

    <!WRONG_EXTERNAL_DECLARATION!>private val b: String<!>

    <!WRONG_EXTERNAL_DECLARATION!>private var c: Float<!>

    <!WRONG_EXTERNAL_DECLARATION!>private var d: Float<!>
        get
        set

    var e: Float
        get
        <!WRONG_EXTERNAL_DECLARATION!>private set<!>

    /*
    private inline fun inline_a(): Int = 23

    private inline val inline_prop: Int
        get() = 42
    */
}

external object O {
    <!WRONG_EXTERNAL_DECLARATION!>private fun a(): Int<!>

    <!WRONG_EXTERNAL_DECLARATION!>private val b: String<!>

    <!WRONG_EXTERNAL_DECLARATION!>private var c: Float<!>

    <!WRONG_EXTERNAL_DECLARATION!>private var d: Float<!>
        get
        set

    /*
    private inline fun inline_a(): Int = 23

    private inline val inline_prop: Int
        get() = 42
    */
}

external class Outer {
    class Inner {
        <!WRONG_EXTERNAL_DECLARATION!>private fun a(): Int<!>

        <!WRONG_EXTERNAL_DECLARATION!>private val b: String<!>

        <!WRONG_EXTERNAL_DECLARATION!>private var c: Float<!>

        <!WRONG_EXTERNAL_DECLARATION!>private var d: Float<!>
            get
            set

        /*
        private inline fun inline_a(): Int = 23

        private inline val inline_prop: Int
            get() = 42
        */
    }

    private class <!WRONG_EXTERNAL_DECLARATION!>PrivateInner<!>
}