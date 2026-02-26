// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84380
// LANGUAGE: -ForbidUselessTypeArgumentsIn25
// WITH_STDLIB

typealias No = List<Int>
typealias Yes<T> = No

typealias No2 = String
typealias Yes2<T> = No2

typealias No3 = Array<String>
typealias Yes3<T> = No3

fun test() {
    Yes::class
    Yes2::class
    Yes3::class

    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Yes<Int>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS_WARNING!>Yes2<Int>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS_WARNING!>Yes3<Int>::class<!>

    Yes<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>::class
    Yes2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING!><Int, String><!>::class
    Yes2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING!><Int, String><!>::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
