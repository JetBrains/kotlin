// TARGET_BACKEND: JVM

fun box(): String =
    object : A<Void, Void>() {
        override fun f(vararg params: Void): Void? = null
    }.execute()

abstract class A<P, R> {
    protected abstract fun f(vararg params: P): R?

    fun execute(vararg params: P): String =
        if (f(*params) == null) "OK" else "Fail"
}
