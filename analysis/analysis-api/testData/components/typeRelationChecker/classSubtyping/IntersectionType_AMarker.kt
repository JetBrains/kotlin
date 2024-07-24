package test

interface Marker

interface A : Marker
interface B

object First : A, B
object Second : A, B

fun <S> intersect(vararg elements: S): S where S : A, S : B = error("")

fun test() = <expr>intersect(First, Second)</expr>

// CLASS_ID: test/Marker
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
