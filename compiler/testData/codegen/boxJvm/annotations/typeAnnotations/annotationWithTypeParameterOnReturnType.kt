// TARGET_BACKEND: JVM_IR
// ISSUE: KT-43553

// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY
// More context at line 12

@Target(AnnotationTarget.TYPE)
annotation class Qualifier<T>

fun <T> func(param: String): @Qualifier<T> String = param

// Type parameter <T> is leaking through return type annotation
// type=@[Qualifier<T of <root>.func>] kotlin.String origin=null
fun box(): String = func<String>("OK")
