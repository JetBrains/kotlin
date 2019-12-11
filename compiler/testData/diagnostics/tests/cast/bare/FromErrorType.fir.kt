// !CHECK_TYPE

class G<T>

fun foo(p: P) {
    val v = p as G?
    checkSubtype<G<*>>(v!!)
}