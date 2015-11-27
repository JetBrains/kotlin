// See also KT-7817

fun <R> synchronized(<!UNUSED_PARAMETER!>lock<!>: Any, block: () -> R): R = block()

class My {
    val test: String
        get() = synchronized(this) {
            var x: String? = ""
            if (x == null) {
                x = "s"
            }
            <!DEBUG_INFO_SMARTCAST!>x<!>
        }
}