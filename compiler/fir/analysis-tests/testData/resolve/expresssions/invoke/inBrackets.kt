// RUN_PIPELINE_TILL: BACKEND
fun test(e: Int.() -> String) {
    val s = 3.e()
    val ss = 3.(e)()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration,
typeWithExtension */
