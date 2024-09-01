package test

interface Marker

interface A
interface B : Marker

object First : A, B
object Second : A, B

fun <S> intersect(vararg elements: S): S where S : A, S : B = error("")

fun test() = <expr>intersect(First, Second)</expr>

val v<caret_type2>2: Marker = First

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Marker
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
