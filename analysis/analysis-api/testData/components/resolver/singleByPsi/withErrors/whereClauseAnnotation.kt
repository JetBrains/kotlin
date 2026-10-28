annotation class Anno(val s: String)

fun <T, U> foo(t: T, u: U) where <expr>@Anno("str")</expr> U : Any {}
