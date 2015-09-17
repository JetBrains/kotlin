package a

import java.util.HashSet

val a: MutableSet<String>? = null
    get() {
        if (a == null) {
            field = HashSet()
        }
        return a
    }

class R {
    val b: String? = null
        get() {
            if (b == null) {
                field = "b"
            }
            return b
        }
}

