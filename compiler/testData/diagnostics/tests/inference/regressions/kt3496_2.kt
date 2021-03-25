// FIR_IDENTICAL
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

import java.util.ArrayList

class F<T> {
    fun <T, V> x (y: F<ArrayList<T>>, w: ArrayList<V>) {
        val z: ArrayList<T> = y["", w]
    }
}
operator fun <V, T> Any.get(s: String, w: ArrayList<V>): ArrayList<T> = throw Exception()