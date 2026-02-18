// ISSUE: KT-84380
// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// LATEST_LV_DIFFERENCE

inline fun <reified R> test() {
    val p1 = R::class
    val p2 = R<!TYPE_ARGUMENTS_NOT_ALLOWED!><Any><!>::class
    val p3 = (R<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int, Int><!>)::class
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, inline, localProperty, nullableType, propertyDeclaration,
reified, typeParameter */
