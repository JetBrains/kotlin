// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

class B {
    var x: Int = 0
        set(<!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> value) {
        field = value
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classDeclaration, classReference, functionDeclaration,
integerLiteral, propertyDeclaration, stringLiteral */
