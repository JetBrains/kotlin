// TARGET_BACKEND: JVM
// WITH_STDLIB

fun box() = X.tag().d()

object X {
    @JvmStatic
    fun d(): String = "OK"

    @JvmStatic
    fun tag(): X = this
}
