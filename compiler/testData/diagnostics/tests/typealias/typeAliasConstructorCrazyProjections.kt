// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Bound<X, Y : X>(val x: X, val y: Y)
typealias B<X, Y> = Bound<X, Y>
typealias BOutIn<T> = Bound<out List<T>, in T>
typealias BInIn<T> = Bound<in List<T>, in T>

fun <T> listOf(): List<T> = null!!

// EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED is ok here:
// we can't create a substituted signature for type alias constructor
// since it has 'out' type projection in 'in' position.
val test1 = <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!><!NI;UNRESOLVED_REFERENCE!>BOutIn<!>(listOf(), null!!)<!>

val test2 = <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>BInIn(listOf(), null!!)<!>