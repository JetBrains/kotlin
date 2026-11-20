annotation class Anno(val s: String)

fun <T, U> foo(t: T, u: U) where @Anno(<expr>"str"</expr>) U : Any {}
