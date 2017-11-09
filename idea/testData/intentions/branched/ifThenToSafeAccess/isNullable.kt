// IS_APPLICABLE: false
fun foo(arg: Any) = if (arg is String?<caret>) arg?.length else null