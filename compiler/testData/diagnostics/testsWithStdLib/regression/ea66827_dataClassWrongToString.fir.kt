data class A(val x: Int) {
    fun toArray(): IntArray =
            intArrayOf(x)

    override fun toString() =
            toArray().takeWhile { it != -1 } // .joinToString()
}
