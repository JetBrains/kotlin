// !DIAGNOSTICS: -NOTHING_TO_INLINE
// TODO: should we disable NOTHING_TO_INLINE in JS backend?
// TODO: uncomment declarations in case we decide to implement KT-14031

external class C {
    private fun a(): Int

    private val b: String

    private var c: Float

    private var d: Float
        get
        set

    var e: Float
        get
        private set

    /*
    private inline fun inline_a(): Int = 23

    private inline val inline_prop: Int
        get() = 42
    */
}

external object O {
    private fun a(): Int

    private val b: String

    private var c: Float

    private var d: Float
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
        private fun a(): Int

        private val b: String

        private var c: Float

        private var d: Float
            get
            set

        /*
        private inline fun inline_a(): Int = 23

        private inline val inline_prop: Int
            get() = 42
        */
    }

    private class PrivateInner
}
