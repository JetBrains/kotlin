// ISSUE: KT-84380
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25

inline fun <reified R> test() {
    val p1 = R::class
    val p2 = R<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Any><!>::class
    val p3 = (R<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int, Int><!>)::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, localProperty, nullableType, propertyDeclaration,
reified, typeParameter */
