// !CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr) {
    val v = tr as G
    checkSubtype<G<*>>(v)
}