// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann2

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann3(val arg: Int, val s: String)

<!WRONG_ANNOTATION_TARGET!>@Ann3(
    <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Ann3(
        <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Ann<!> 5, ""
    )<!> <!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Ann2<!> 1, ""
)<!> val a = 0

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */
