// !WITH_NEW_INFERENCE
class Bound<X, Y : X>(val x: X, val y: Y)
typealias B<X, Y> = Bound<X, Y>
typealias BOutIn<T> = Bound<out List<T>, in T>
typealias BInIn<T> = Bound<in List<T>, in T>

fun <T> listOf(): List<T> = null!!

// Unresolved reference is ok here:
// we can't create a substituted signature for type alias constructor
// since it has 'out' type projection in 'in' position.
val test1 = <!UNRESOLVED_REFERENCE!>BOutIn<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>listOf<!>(), null!!)

val test2 = <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>BInIn(listOf(), null!!)<!>