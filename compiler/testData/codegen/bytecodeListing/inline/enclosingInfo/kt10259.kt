// FILE: 1.kt

package test

inline fun test(s: () -> Unit) {
    s()
}

// FILE: 2.kt

import test.*

fun box() {
    var s1 = ""
    var s2 = ""
    test {
        {
            val p = object {}
            s1 = p.toString();
            {
                val q = object {}
                s2 = q.toString()
            }()
        }()
    }
}
