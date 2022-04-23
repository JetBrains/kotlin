// MODULE: lib
class Q<T : Q.S> {
    open class S {
        val ok = "OK"
    }
}
// MODULE: main(lib)
fun box() = Q.S().ok