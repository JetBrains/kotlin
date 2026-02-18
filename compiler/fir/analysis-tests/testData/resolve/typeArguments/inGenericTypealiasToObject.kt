// ISSUE: KT-84281, KT-84336, KT-84380
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25, +ProperSupportOfInnerClassesInCallableReferenceLHS
//                                            ^ otherwise, different positioning for one of the diagnostics

object Obj

typealias TObj<K> = Obj
typealias TUnit<K> = Unit

fun test() {
    val p1 = TObj
    val p2 = TUnit

    val p3 = TObj<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Nothing><!>
    val p4 = TUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Nothing><!>

    val p5 = TObj<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Int><!>
    val p6 = TUnit<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int, Int><!>

    val p7 = TObj::class
    val p8 = TUnit::class

    val p9 = TObj<Int>::class
    val p10 = TUnit<Int>::class

    val p11 = TObj::toString
    val p12 = TUnit::toString

    val p13 = TObj<Int>::toString
    val p14 = TUnit<Int>::<!UNRESOLVED_REFERENCE!>toString<!>

    val p15 = TObj<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::toString
    val p16 = TUnit<Int, Int>::<!UNRESOLVED_REFERENCE!>toString<!>

    val p17 = TObj.toString()
    val p18 = TUnit.toString()

    val p19 = TObj<Int>.<!UNRESOLVED_REFERENCE!>toString<!>()
    val p20 = TUnit<Int>.<!UNRESOLVED_REFERENCE!>toString<!>()

    val p21 = TObj<Int, Int, Int>::class
    val p22 = TUnit<Int, Int, Int>::class
}

/* GENERATED_FIR_TAGS: callableReference, classReference, functionDeclaration, localProperty, nullableType,
objectDeclaration, propertyDeclaration, starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
