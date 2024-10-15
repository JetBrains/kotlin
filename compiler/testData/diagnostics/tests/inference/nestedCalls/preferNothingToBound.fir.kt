// RUN_PIPELINE_TILL: BACKEND
fun <K> id(arg: K): K = arg

val v = id(null)
