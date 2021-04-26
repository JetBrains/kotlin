// FILE: 1.kt

package test

inline fun test(s: () -> Unit) {
    s()
}

// FILE: 2.kt

import test.*

fun <T> eval(f: () -> T) = f()

fun box() {
    var s1 = ""
    var s2 = ""
    test {
        eval {
            val p = object {}
            s1 = p.toString();
            eval {
                val q = object {}
                s2 = q.toString()
            }
        }
    }
}
