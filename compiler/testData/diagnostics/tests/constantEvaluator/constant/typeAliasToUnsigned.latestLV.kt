// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

typealias UI = UInt

const val a: UI = 1u
const val b: UI = a
const val c = a == b

/* GENERATED_FIR_TAGS: const, equalityExpression, propertyDeclaration, typeAliasDeclaration, unsignedLiteral */
