// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

import D.*

class D {
    object A
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
object A

fun test() {
    <!DEPRECATION_ERROR!>A<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, objectDeclaration, stringLiteral */
