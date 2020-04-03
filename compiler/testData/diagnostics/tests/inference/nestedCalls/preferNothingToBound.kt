fun <K> id(arg: K): K = arg

val v = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>id(null)<!>
