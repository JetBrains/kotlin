// ISSUE: KT-84167
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25
// DISABLE_NEXT_PHASE_SUGGESTION

// FILE: part1/part2/part3/tests.kt

package part1.part2.part3

annotation class Anno
annotation class AnnoWithArg(val s: String)

@part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.Anno
fun target1() {
}

@part1.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3.Anno
fun target2() {
}

@part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2.part3.AnnoWithArg("string")
fun target3() {
}

@part1<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part2<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.part3<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>.AnnoWithArg("string")
fun target4() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
