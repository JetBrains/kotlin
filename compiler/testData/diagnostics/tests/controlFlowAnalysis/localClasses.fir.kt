package f

fun f() {
    class LocalClass() {
        init {
            val x1 = "" // ok: unused

            fun loc1(): Int {
                val x1_ = "" // ok: unused
            <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
        }

        fun f() {
            val x2 = "" // error: should be UNUSED_VARIABLE

            fun loc2(): Int {
                val x2_ = "" // error: should be UNUSED_VARIABLE
            <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
        }

        val v: String
            get() {
                val x3 = "" // ok: unused
            <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
    }
}
