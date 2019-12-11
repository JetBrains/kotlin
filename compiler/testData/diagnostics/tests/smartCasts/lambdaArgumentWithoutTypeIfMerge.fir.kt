// See also KT-7817

fun <R> synchronized(lock: Any, block: () -> R): R = block()

class My {
    val test: String
        get() = synchronized(this) {
            var x: String? = ""
            if (x == null) {
                x = "s"
            }
            x
        }
}