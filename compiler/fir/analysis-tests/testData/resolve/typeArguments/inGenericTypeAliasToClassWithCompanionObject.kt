// ISSUE: KT-84281
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25, +ProperSupportOfInnerClassesInCallableReferenceLHS

class WithCompanion {
    fun M() { }
    companion object {
        fun S() { }
    }
}

typealias TypeAlias<K> = WithCompanion

fun test() {
    val p1 = TypeAlias
    val p2 = <!NO_COMPANION_OBJECT!>TypeAlias<Int><!>
    val p3 = <!NO_COMPANION_OBJECT!>TypeAlias<Int, Int><!>

    val p4 = TypeAlias.toString()
    val p5 = TypeAlias<Int>.<!UNRESOLVED_REFERENCE!>toString<!>()

    val p6 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypeAlias<!>::toString
    val p7 = TypeAlias<Int>::toString

    val p8 = TypeAlias::class
    val p9 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>TypeAlias<Int>::class<!>

    val p10 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypeAlias<!>::S
    val p11 = TypeAlias<Int>::<!UNRESOLVED_REFERENCE!>S<!>

    val p12 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>TypeAlias<!>::M
    val p13 = TypeAlias<Int>::M
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, companionObject, functionDeclaration,
localProperty, nullableType, objectDeclaration, propertyDeclaration, starProjection, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
