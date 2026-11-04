annotation class Anno(val s: String)

fun <T> foo(t: T) where <expr>@Anno("str")</expr> AA : Any {}
