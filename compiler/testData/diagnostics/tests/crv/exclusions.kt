// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""
fun unitF(): Unit = Unit

fun exlusionPropagation(cond: Boolean, m: MutableList<String>) {
    if (cond) m.add("x") else throw IllegalStateException()
    if (cond) stringF() else throw IllegalStateException()
}

@IgnorableReturnValue
fun discardable(): String = ""

fun unused(cond: Boolean) {
    stringF()
    discardable()
    if (cond) discardable() else stringF()
    if (cond) discardable() else unitF()
}

fun underscore() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = stringF()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, safeCall, stringLiteral, unnamedLocalVariable */
