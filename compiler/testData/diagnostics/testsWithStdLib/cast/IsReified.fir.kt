// RUN_PIPELINE_TILL: BACKEND
fun ff(a: Any) = a is Array<*> && a.isArrayOf<String>()