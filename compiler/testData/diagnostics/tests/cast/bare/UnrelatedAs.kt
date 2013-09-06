trait Tr
trait G<T>

fun test(tr: Tr) {
    val v = tr as G
    v: G<*>
}
