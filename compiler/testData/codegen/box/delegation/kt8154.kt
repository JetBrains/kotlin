// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun foo(): T
}

interface B<T> : A<T>

class BImpl<T>(a: A<T>) : B<T>, A<T> by a

fun box(): String {
    val b: B<String> = BImpl(object : A<String> {
        override fun foo() = "OK"
    })

    if (b.foo() != "OK") return "fail 1"

    val a: A<String> = b

    return a.foo()
}
