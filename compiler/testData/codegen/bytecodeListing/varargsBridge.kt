abstract class A<T> {
    protected abstract fun doIt(vararg args: T): String
    fun test() = doIt()
}

class B : A<Void>() {
    override fun doIt(vararg args: Void): String = "OK"
}
