// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE_FEATURE_TOGGLED: AllowAnnotationsOnArgumentsOfAnnotations

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
    @Ann3(
        @Ann 5, ""
    ) @Ann2 1, ""
)<!> val a = 0

/* GENERATED_FIR_TAGS: annotationDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */
