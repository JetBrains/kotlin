// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

const val longVal1 = 2L.inc()
const val longVal2 = 2L.dec()
const val longVal3 = Long.MAX_VALUE.inc()
const val longVal4 = Long.MIN_VALUE.dec()

const val intVal1 = 2.inc()
const val intVal2 = 2.dec()
const val intVal3 = Int.MAX_VALUE.inc()
const val intVal4 = Int.MIN_VALUE.dec()

const val shortVal1 = 2.toShort().inc()
const val shortVal2 = 2.toShort().dec()
const val shortVal3 = Short.MAX_VALUE.inc()
const val shortVal4 = Short.MIN_VALUE.dec()

const val byteVal1 = 2.toByte().inc()
const val byteVal2 = 2.toByte().dec()
const val byteVal3 = Byte.MAX_VALUE.inc()
const val byteVal4 = Byte.MIN_VALUE.dec()

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
