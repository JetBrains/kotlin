// WITH_SIGNATURES

class A<T>(val result: T) {
    fun b() {
        class C<S> {
            fun f() {
                fun g(t: T): S? = null
            }
        }
    }
}
