// FILE: A.kt

open class A<T>(val x: T)

// FILE: AFactory.kt

abstract class AFactory {
    abstract fun create(): A<Int>?
}

// FILE: Util.kt

inline fun <reified T> createWith(x: T, f: (T) -> A<T>?)
        = f(x)

// FILE: B.kt

class B(x: Int) : A<Int>(x) {
    companion object : AFactory() {
        override fun create(): A<Int>? = createWith(0, ::B)
    }
}
