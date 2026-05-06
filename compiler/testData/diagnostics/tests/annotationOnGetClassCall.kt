// RUN_PIPELINE_TILL: FRONTEND
annotation class Ann(val x: Long, val s: String)

fun test() {
    <!WRONG_ANNOTATION_TARGET!>@Ann(s = "hello", x = 1)<!> String::class
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
