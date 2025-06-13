// RUN_PIPELINE_TILL: BACKEND
class My(x: String) {
    val y: String = <!DEBUG_INFO_LEAKING_THIS!>foo<!>(x)

    fun foo(x: String) = "$x$y"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration */
