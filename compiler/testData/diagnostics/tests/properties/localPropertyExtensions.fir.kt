// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

fun test() {
    val <!LOCAL_EXTENSION_PROPERTY!>String<!>.count = 42
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
propertyWithExtensionReceiver */
