// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

const val minusOneVal = (-1).toShort()
const val zeroVal = 0.toShort()
const val oneVal = 1.toShort()
const val twoVal = 2.toShort()
const val threeVal = 3.toShort()
const val fourVal = 4.toShort()

const val and1 = oneVal.and(twoVal)
const val and2 = twoVal.and(twoVal)
const val and3 = threeVal.and(twoVal)
const val and4 = 12.toShort().and(10.toShort())

const val or1 = oneVal.or(twoVal)
const val or2 = twoVal.or(twoVal)
const val or3 = threeVal.or(twoVal)
const val or4 = 12.toShort().or(10.toShort())

const val xor1 = oneVal.xor(twoVal)
const val xor2 = twoVal.xor(twoVal)
const val xor3 = threeVal.xor(twoVal)
const val xor4 = 12.toShort().xor(10.toShort())

const val inv1 = zeroVal.inv()
const val inv2 = oneVal.inv()

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
