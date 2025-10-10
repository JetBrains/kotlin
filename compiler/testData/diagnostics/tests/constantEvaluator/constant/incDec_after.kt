// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

const val longVal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2L.inc()<!>
const val longVal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2L.dec()<!>
const val longVal3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Long.MAX_VALUE.inc()<!>
const val longVal4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Long.MIN_VALUE.dec()<!>

const val intVal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.inc()<!>
const val intVal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.dec()<!>
const val intVal3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Int.MAX_VALUE.inc()<!>
const val intVal4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Int.MIN_VALUE.dec()<!>

const val shortVal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toShort().inc()<!>
const val shortVal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toShort().dec()<!>
const val shortVal3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Short.MAX_VALUE.inc()<!>
const val shortVal4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Short.MIN_VALUE.dec()<!>

const val byteVal1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toByte().inc()<!>
const val byteVal2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>2.toByte().dec()<!>
const val byteVal3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Byte.MAX_VALUE.inc()<!>
const val byteVal4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Byte.MIN_VALUE.dec()<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
