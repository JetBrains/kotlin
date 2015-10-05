// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE

import java.util.ArrayList

class F<T> {
    fun <T, V> x (y: F<ArrayList<T>>, w: ArrayList<V>) {
        val z: ArrayList<T> = y["", w]
    }
}
operator fun <V, T> Any.get(<!UNUSED_PARAMETER!>s<!>: String, <!UNUSED_PARAMETER!>w<!>: ArrayList<V>): ArrayList<T> = throw Exception()