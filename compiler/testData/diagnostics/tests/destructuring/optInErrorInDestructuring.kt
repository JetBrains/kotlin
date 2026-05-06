// RUN_PIPELINE_TILL: FRONTEND
@RequiresOptIn
annotation class MyOptIn

@MyOptIn
data class OptInData(val a: String)

fun reproduceIssue() {
    val (<!OPT_IN_USAGE_ERROR!>x<!>) = <!OPT_IN_USAGE_ERROR!>OptInData<!>("1")
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, destructuringDeclaration, functionDeclaration,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
