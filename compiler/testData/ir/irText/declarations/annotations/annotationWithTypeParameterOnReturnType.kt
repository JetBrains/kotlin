// ISSUE: KT-85775

@Target(AnnotationTarget.TYPE)
annotation class Qualifier<T>

fun <T> func(param: String): @Qualifier<T> String = param

fun foo(): String = func<String>("OK")
