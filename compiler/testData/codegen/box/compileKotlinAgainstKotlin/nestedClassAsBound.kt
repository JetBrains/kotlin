// MODULE: lib
// FILE: Q.kt
class Q<T : Q.S> {
    open class S {
        val ok = "OK"
    }
}
// MODULE: main(lib)
// FILE: box.kt
fun box() = Q.S().ok