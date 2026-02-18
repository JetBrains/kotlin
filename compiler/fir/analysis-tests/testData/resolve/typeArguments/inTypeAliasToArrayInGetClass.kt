// ISSUE: KT-84380
// RUN_PIPELINE_TILL: FRONTEND

typealias G<K> = Array<K>
typealias R = Array<String>

fun testJVM() {
    val p1 = G::class
    val p2 = R::class

    val p3 = G<Int>::class
    val p4 = R<Int>::class

    val p5 = G<Int, Int>::class
    val p6 = R<Int, Int>::class

    Array<Int, Int>::class
    Array<Int>::class
    Array::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, localProperty, nullableType, propertyDeclaration,
starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
