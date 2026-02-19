// LANGUAGE: +SkipHiddenObjectsInResolution
// FIR_IDENTICAL
//  ^ K1 is ignored
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82555

import D.*

class D {
    object A
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
object A

fun test() {
    A
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, objectDeclaration, stringLiteral */
