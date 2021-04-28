// WITH_SIGNATURES

class A<T>(val result: T) {
    fun f(): T {
        fun g(): T = result
        return g()
    }
}
