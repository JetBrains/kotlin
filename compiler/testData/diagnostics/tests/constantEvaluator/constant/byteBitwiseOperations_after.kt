// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

const val minusOneVal = (-1).toByte()
const val zeroVal = 0.toByte()
const val oneVal = 1.toByte()
const val twoVal = 2.toByte()
const val threeVal = 3.toByte()
const val fourVal = 4.toByte()

const val and1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.and(twoVal)<!>
const val and2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.and(twoVal)<!>
const val and3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.and(twoVal)<!>
const val and4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12.toByte().and(10.toByte())<!>

const val or1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.or(twoVal)<!>
const val or2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.or(twoVal)<!>
const val or3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.or(twoVal)<!>
const val or4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12.toByte().or(10.toByte())<!>

const val xor1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.xor(twoVal)<!>
const val xor2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>twoVal.xor(twoVal)<!>
const val xor3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>threeVal.xor(twoVal)<!>
const val xor4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>12.toByte().xor(10.toByte())<!>

const val inv1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>zeroVal.inv()<!>
const val inv2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>oneVal.inv()<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
