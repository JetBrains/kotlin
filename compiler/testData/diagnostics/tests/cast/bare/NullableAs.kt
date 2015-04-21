// !CHECK_TYPE

trait Tr<T>
trait G<T> : Tr<T>

fun test(tr: Tr<String>?) {
    val v = tr as G
    checkSubtype<G<String>>(v)
}