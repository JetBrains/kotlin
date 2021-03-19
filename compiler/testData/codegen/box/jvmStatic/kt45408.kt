// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun box() = X.tag().d()

object X {
    @JvmStatic
    fun d(): String = "OK"

    @JvmStatic
    fun tag(): X = this
}
