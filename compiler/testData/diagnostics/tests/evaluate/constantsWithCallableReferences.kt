// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

enum class EnumClass {
    VALUE,
}

val trimRef: () -> String = "  trim me  "::trim
val lowercaseRef: () -> String = "LOWERCASE"::lowercase

const val trim = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>trimRef.invoke()<!>
const val lowercase = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>lowercaseRef.invoke()<!>
const val enumName = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>EnumClass.VALUE::name.invoke()<!>
const val char = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>::Char.invoke(42)<!>
const val byteInc = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.toByte()::inc.invoke()<!>
const val uByteInc = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u.toUByte()::inc.invoke()<!>

/* GENERATED_FIR_TAGS: callableReference, const, enumDeclaration, enumEntry, integerLiteral, propertyDeclaration,
stringLiteral, unsignedLiteral */
