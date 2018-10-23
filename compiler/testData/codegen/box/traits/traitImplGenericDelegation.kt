// IGNORE_BACKEND: JS_IR
interface A<T, U : Number, V : Any> {
    fun foo(t: T, u: U): V? {
        return null
    }
}

interface B<T, V : Any> : A<T, Int, V>

class C : B<String, Runnable> {
    override fun foo(t: String, u: Int): Runnable? {
        return super.foo(t, u)
    }
}

interface Runnable {
    fun run(): Unit
}

fun box(): String {
    val x = C().foo("", 0)
    return if (x == null) "OK" else "Fail: $x"
}
