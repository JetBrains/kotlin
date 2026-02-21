// ISSUE: KT-84380
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25

typealias G<K> = Array<K>
typealias R = Array<String>

fun testJVM() {
    val p1 = G::class
    val p2 = R::class

    val p3 = G<Int>::class
    val p4 = R<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING!><Int><!>::class

    val p5 = G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING!><Int, Int><!>::class
    val p6 = R<!WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING!><Int, Int><!>::class

    Array<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::class
    Array<Int>::class
    Array::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, localProperty, nullableType, propertyDeclaration,
starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
