// ISSUE: KT-84380, KT-84281
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25,  +ProperSupportOfInnerClassesInCallableReferenceLHS
//                                              ^ otherwise, different positioning for one of the diagnostics

object SomeObject {
    fun foo(){}
}

typealias A1 = SomeObject
typealias A2 = A1
typealias A3<T> = A2

fun test() {
    A2
    A2<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Any><!>

    A2::class
    A2::foo

    A2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::class
    A2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any><!>::foo

    A3::class
    A3::foo

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A3<Any>::class<!>
    A3<Any>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classReference, functionDeclaration, nullableType, objectDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
