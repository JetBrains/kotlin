// FIR_IDENTICAL
//  ^ K1 is ignored
// LANGUAGE: +SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 2.3

class C {
    @SinceKotlin("2.4")
    companion object
}

fun test() {
    <!API_NOT_AVAILABLE!>C<!>
    <!API_NOT_AVAILABLE!>C<!>.toString()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, stringLiteral */
