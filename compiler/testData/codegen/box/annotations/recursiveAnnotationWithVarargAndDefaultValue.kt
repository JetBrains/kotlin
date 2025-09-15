// ISSUE: KT-80908
// IGNORE_BACKEND_K1: ANY

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(vararg val arg: @Ann("a") String = [])

fun box(): String {
    val a = Ann("O", "K")
    val arg = a.arg
    return arg[0] + arg[1]
}
