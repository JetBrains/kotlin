// !CHECK_TYPE

class G<T>

fun foo(p: <!UNRESOLVED_REFERENCE!>P<!>) {
    val v = p as G?
    checkSubtype<G<*>>(v!!)
}