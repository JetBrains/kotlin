// !CHECK_TYPE

interface B<T>
interface G<T>: B<T>

fun f(p: <!OTHER_ERROR, OTHER_ERROR!>B<Foo><!>): Any {
    val v = p as G
    return checkSubtype<G<*>>(v)
}