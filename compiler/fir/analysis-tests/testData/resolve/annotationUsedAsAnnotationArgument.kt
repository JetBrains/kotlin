annotation class Ann

annotation class Ann2

annotation class Ann3(val arg: Int, val s: String)

@Ann3(
    <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann3(
        <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann<!> 5, ""
    )<!> <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Ann2<!> 1, ""
) val a = 0