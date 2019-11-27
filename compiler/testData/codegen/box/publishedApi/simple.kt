// IGNORE_BACKEND_FIR: JVM_IR
// MODULE: lib
// FILE: lib.kt
class A {

    @PublishedApi
    internal fun published() = "OK"

    inline fun test() = published()

}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A().test()
}