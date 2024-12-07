// RUN_PIPELINE_TILL: BACKEND
fun ff(a: Any) = a is Array<*> && <!DEBUG_INFO_SMARTCAST!>a<!>.isArrayOf<String>()