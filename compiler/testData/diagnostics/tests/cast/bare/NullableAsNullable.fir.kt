// !CHECK_TYPE

interface Tr<T>
interface G<T> : Tr<T>

fun test(tr: Tr<String>?) {
    val v = tr as G?
    // If v is not nullable, there will be a warning on this line:
    checkSubtype<G<String>>(v!!)
}