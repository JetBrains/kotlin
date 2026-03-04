// TARGET_BACKEND: JVM_IR
// ISSUE: KT-43553

@Target(AnnotationTarget.TYPE)
annotation class Qualifier<T>

fun <T> func(param: String): @Qualifier<T> String = param

fun box(): String = func<String>("OK")
