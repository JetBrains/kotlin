data class A(val x: Int) {
    fun toArray(): IntArray =
            intArrayOf(x)

    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>toString<!>() =
            toArray().takeWhile { it != -1 } // .joinToString()
}
