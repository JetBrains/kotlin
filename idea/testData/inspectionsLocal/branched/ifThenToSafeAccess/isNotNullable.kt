// PROBLEM: none
fun foo(arg: Any) = if (arg !is String?<caret>) null else arg?.length