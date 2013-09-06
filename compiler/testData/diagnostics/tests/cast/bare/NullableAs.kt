trait Tr<T>
trait G<T> : Tr<T>

fun test(tr: Tr<String>?) {
    val v = tr as G
    v: G<String>
}