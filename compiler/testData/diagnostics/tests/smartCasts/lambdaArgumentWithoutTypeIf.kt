// See also KT-7800

fun <T, R> T.let(f: (T) -> R): R = f(this)

fun foo(): Int {
    val x: Int = 1.let {
        val value: Int? = null
        if (value == null) {
            return@let 1
        }

        <!DEBUG_INFO_SMARTCAST!>value<!> // smart-cast should be here
    }
    return x
}