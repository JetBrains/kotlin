// ISSUE: KT-84380
// RUN_PIPELINE_TILL: BACKEND

inline fun <reified R> test() {
    val p1 = R::class
    val p2 = R<Any>::class
    val p3 = (R<Int, Int>)::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, localProperty, nullableType, propertyDeclaration,
reified, typeParameter */
