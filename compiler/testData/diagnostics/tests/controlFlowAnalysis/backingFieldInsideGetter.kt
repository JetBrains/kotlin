package a

import java.util.HashSet

val a: MutableSet<String>? = null
    get() {
        if (a == null) {
            $a = HashSet()
        }
        return a
    }

class R {
    val b: String? = null
        get() {
            if (b == null) {
                $b = "b"
            }
            return b
        }
}

