// IGNORE_BACKEND_FIR: JVM_IR
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Annotation

fun box(): String {
    var v = 0
    @Annotation v += 1 + 2
    if (v != 3) return "fail1"

    @Annotation v = 4
    if (v != 4) return "fail2"

    return "OK"
}