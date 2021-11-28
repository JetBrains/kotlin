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
    <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann3(
        <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann<!> 5, ""
    )<!> <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann2<!> 1, ""
)<!> val a = 0
