// LANGUAGE: -SkipHiddenObjectsInResolution
// RUN_PIPELINE_TILL: FRONTEND


class C {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    companion object
}

fun test() {
    <!DEPRECATION_ERROR!>C<!>
    <!DEPRECATION_ERROR!>C<!>.toString()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, stringLiteral */
