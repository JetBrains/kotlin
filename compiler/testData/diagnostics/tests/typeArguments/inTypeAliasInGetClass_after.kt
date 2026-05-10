// ISSUE: KT-84380
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25

class C
object O
class G<T>

typealias TC = C
typealias TO<K> = O
typealias TG<K> = G<K>

fun test() {
    TC::class
    TO::class
    TG::class

    TC<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::class
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>TO<Int>::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>TG<Int>::class<!>

    TC<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::class
    TO<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::class
    TG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, objectDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
