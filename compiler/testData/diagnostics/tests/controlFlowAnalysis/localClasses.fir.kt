package f

fun f() {
    class LocalClass() {
        init {
            val x1 = "" // ok: unused

            fun loc1(): Int {
                val x1_ = "" // ok: unused
            }
        }

        fun f() {
            val x2 = "" // error: should be UNUSED_VARIABLE

            fun loc2(): Int {
                val x2_ = "" // error: should be UNUSED_VARIABLE
            }
        }

        val v: String
            get() {
                val x3 = "" // ok: unused
            }
    }
}
