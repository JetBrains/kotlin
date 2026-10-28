annotation class Anno(val s: String)

fun <T> foo(t: T) where @Anno(<expr>"str"</expr>) AA : Any {}
