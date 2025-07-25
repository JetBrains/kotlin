// RUN_PIPELINE_TILL: FRONTEND
// This test exists only to check that we don't accidentally break the buggy behavior of the old JVM backend in JVM IR (KT-42321).
// Feel free to remove it as soon as there's no language version where such code is allowed (KT-38895).

abstract class C<L> {
    abstract fun takeT(x: L)
}

fun testLongDotCall(c1: C<Long>) {
    c1.takeT(1.plus(2))
    c1.takeT(1.minus(2))
    c1.takeT(1.times(2))
    c1.takeT(1.div(2))
    c1.takeT(1.rem(2))
    c1.takeT(<!TYPE_MISMATCH!>1.inc()<!>)
    c1.takeT(<!TYPE_MISMATCH!>1.dec()<!>)
    c1.takeT(1.unaryPlus())
    c1.takeT(1.unaryMinus())
    c1.takeT(1.shl(2))
    c1.takeT(1.shr(2))
    c1.takeT(1.ushr(2))
    c1.takeT(1.and(2))
    c1.takeT(1.or(2))
    c1.takeT(1.xor(2))
    c1.takeT(1.inv())
}

fun testShortDotCall(c2: C<Short>) {
    c2.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.plus(2)<!>)
    c2.takeT(<!TYPE_MISMATCH!>1.inc()<!>)
    c2.takeT(<!TYPE_MISMATCH!>1.dec()<!>)
    c2.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.shr(2)<!>)
    c2.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.inv()<!>)
}

fun testByteDotCall(c3: C<Byte>) {
    c3.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.plus(2)<!>)
    c3.takeT(<!TYPE_MISMATCH!>1.inc()<!>)
    c3.takeT(<!TYPE_MISMATCH!>1.dec()<!>)
    c3.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.shr(2)<!>)
    c3.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1.inv()<!>)
}

fun testLongOperatorInfixCall(c4: C<Long>) {
    c4.takeT(1 + 2)
    c4.takeT(1 - 2)
    c4.takeT(1 * 2)
    c4.takeT(1 / 2)
    c4.takeT(1 % 2)
    c4.takeT(+1)
    c4.takeT(-1)
    c4.takeT(1 shl 2)
    c4.takeT(1 shr 2)
    c4.takeT(1 ushr 2)
    c4.takeT(1 and 2)
    c4.takeT(1 or 2)
    c4.takeT(1 xor 2)
}

fun testShortOperatorInfixCall(c5: C<Short>) {
    c5.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!>)
    c5.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 shr 2<!>)
}

fun testByteOperatorInfixCall(c6: C<Byte>) {
    c6.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!>)
    c6.takeT(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 shr 2<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, typeParameter */
