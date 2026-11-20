@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

fun <T> foo(x: T): T = x

val x = foo<@Anno(<expr>"a"</expr>) _>(1)
