// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

open class B {
    open fun foo(a: Int = 0, b: Int = 1) {}
}

class D : B() {
    override fun <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>foo<!>(
    a: Int,
    @IntroducedAt("1") b: Int = <!DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE!>1<!>,
    )
    {}
}


/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
integerLiteral, override, stringLiteral */
