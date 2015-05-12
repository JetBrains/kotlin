//KT-1191 Wrong detection of unused parameters
package kt1191

interface FunctionalList<T> {
    val size: Int
    val head: T
    val tail: FunctionalList<T>
}

fun <T> FunctionalList<T>.plus(element: T) : FunctionalList<T> = object: FunctionalList<T> {
    override val size: Int
    get() = 1 + this@plus.size
    override val tail: FunctionalList<T>
    get() = this@plus
    override val head: T
    get() = element
}

fun foo(unused: Int) = object {
    val a : Int get() = unused
}
