// RUN_PIPELINE_TILL: FRONTEND

val x = object {
    fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>f<!>(a: Int = 1, @IntroducedAt("1") b: String = "x") = "$a/$b"
}


fun outer() {
    fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>local<!>(a: Int = 0, @IntroducedAt("1") b: Int = 1) {
    }
}

fun outerWithLocalClass() {
    class Local {
        fun <!INVALID_VERSIONING_ON_LOCAL_FUNCTION!>member<!>(
            value: String,
            @IntroducedAt("1") suffix: String = "!",
        ) = value + suffix
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, anonymousObjectExpression, classReference, functionDeclaration,
integerLiteral, localFunction, propertyDeclaration, stringLiteral */
