// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

class B {
    var x: Int = 0
        set(@IntroducedAt("1") value) {
        field = value
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classDeclaration, classReference, functionDeclaration,
integerLiteral, propertyDeclaration, stringLiteral */
