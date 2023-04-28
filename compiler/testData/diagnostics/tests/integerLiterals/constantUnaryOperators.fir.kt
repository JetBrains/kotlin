// SKIP_TXT
// FIR_DUMP

// ------------- const -------------

val i1 = (2 + 2 * 3).inv()
val l1: Long = (2 + 2 * 3).inv()
val ll1 = (3000000000 * 2 + 1).inv()

val i2 = (2 + 2 * 3).unaryPlus()
val l2: Long = (2 + 2 * 3).unaryPlus()
val ll2 = (3000000000 * 2 + 1).unaryPlus()

val i3 = (2 + 2 * 3).unaryMinus()
val l3: Long = (2 + 2 * 3).unaryMinus()
val ll3 = (3000000000 * 2 + 1).unaryMinus()

// ------------- non const -------------

val i4 = (2 + 2 * 3).inc()
val l4: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(2 + 2 * 3).inc()<!>
val ll4 = (3000000000 * 2 + 1).inc()

val i5 = (2 + 2 * 3).dec()
val l5: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(2 + 2 * 3).dec()<!>
val ll5 = (3000000000 * 2 + 1).dec()
