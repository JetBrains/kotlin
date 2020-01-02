// See also KT-7800

fun foo(): Int {
    val x: Int = 1.let {
        val value: Int? = null
        if (value == null) {
            return@let 1
        }

        value // smart-cast should be here
    }
    return x
}