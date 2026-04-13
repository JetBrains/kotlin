// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

val x = object {
    fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>f<!>(a: Int = 1, @IntroducedAt("1") b: String = "x") = "$a/$b"
}


fun outer() {
    @OptIn(ExperimentalVersionOverloading::class)
    fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>local<!>(a: Int = 0, @IntroducedAt("1") b: Int = 1) {
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, anonymousObjectExpression, classReference, functionDeclaration,
integerLiteral, localFunction, propertyDeclaration, stringLiteral */
