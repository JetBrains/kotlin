@Target(AnnotationTarget.TYPE)
annotation class Anno5(val s: String)

fun f<caret>oo(): List<@Anno5("1") Int>? = null
