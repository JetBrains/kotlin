// ISSUE: KT-85641
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

object Obj {
    fun f1() = Unit
    fun g() = Unit
}

fun Obj?.g() = Unit

fun Obj.f2() = Unit
fun Obj?.f3() = Unit

typealias TObj = Obj
typealias TNObj = Obj?

typealias GTObj<K> = Obj
typealias GTNObj<K> = Obj?

val `=====` = Unit

fun test() {
    val p1 = Obj?::f1
    val p2 = Obj?::f2
    val p3 = Obj?::f3
    `=====`
    val p4 = TObj?::f1
    val p5 = TObj?::f2
    val p6 = TObj?::f3
    `=====`
    val p7 = TNObj?::f1
    val p8 = TNObj?::f2
    val p9 = TNObj?::f3
    `=====`
    val p10 = TNObj::f1
    val p11 = TNObj::f2
    val p12 = TNObj::f3
    `=====`
    val p13 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTObj<!>?::f1
    val p14 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTObj<!>?::f2
    val p15 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTObj<!>?::f3
    `=====`
    val p16 = GTObj<*>?::<!UNSAFE_CALLABLE_REFERENCE!>f1<!>
    val p17 = GTObj<*>?::<!UNSAFE_CALLABLE_REFERENCE!>f2<!>
    val p18 = GTObj<*>?::f3
    `=====`
    val p19 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTNObj<!>?::f1
    val p20 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTNObj<!>?::f2
    val p21 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GTNObj<!>?::f3
    `=====`
    val p22 = GTNObj<*>?::<!UNSAFE_CALLABLE_REFERENCE!>f1<!>
    val p23 = GTNObj<*>?::<!UNSAFE_CALLABLE_REFERENCE!>f2<!>
    val p24 = GTNObj<*>?::f3
    `=====`
    val p25 = Obj?::g
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, localProperty, nullableType,
objectDeclaration, propertyDeclaration, starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
