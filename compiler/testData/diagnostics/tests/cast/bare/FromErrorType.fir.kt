// !CHECK_TYPE

class G<T>

fun foo(p: <!OTHER_ERROR, OTHER_ERROR!>P<!>) {
    val v = p as G?
    checkSubtype<G<*>>(v!!)
}