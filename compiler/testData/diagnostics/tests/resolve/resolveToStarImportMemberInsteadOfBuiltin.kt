// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// https://youtrack.jetbrains.com/issue/KT-48157

import TestEnum.*

enum class TestEnum {
    Annotation,
    Collection,
    Map,
    Function,
    Enum
}

fun test() {
    val x1 = Annotation
    val x2 = Collection
    val x3 = Map
    val x4 = Function
    val x5 = Enum
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
