// RUN_PIPELINE_TILL: FRONTEND
fun String.f() {
    <!SUPER_NOT_AVAILABLE!>super@f<!>.compareTo("")
    <!SUPER_NOT_AVAILABLE!>super<!>.compareTo("")
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, stringLiteral */
