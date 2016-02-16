// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH
// !MESSAGE_TYPE: HTML

package a.b.c

class G

class D<T, G> {
    fun f(t: Map<T, a.b.c.G>) {
    }

    fun <T> f(t1: Map<T, G>, T2: T) {
        f(t1)
    }
}