// WITH_STDLIB

package test

class A internal constructor(a: Int = 0) {
    internal fun internalFunction(b: String = "") {}

    @JvmName("internalJvmNameFunction")
    internal fun f(c: String = "") {}

    public fun publicFunction(d: String = "") {}
}
