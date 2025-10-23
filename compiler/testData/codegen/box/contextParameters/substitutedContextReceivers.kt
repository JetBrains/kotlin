// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class Box<E>(val x: E)

class A<X, Y : Number> {
    context(box: Box<X>, y: Y)
    fun foo(): String = box.x.toString() + y.toString()

    context(box: Box<X>, y: Y)
    val p1: String get() = box.x.toString() + y.toString()
}

context(box: Box<X>, y: Y)
fun <X, Y : Number> bar(): String = box.x.toString() + y.toString()

fun box(): String {
    return with(Box("OK")) {
        with(56) {
            val a = A<String, Int>()
            if (a.foo() != "OK56") return "fail 1"
            if (a.p1 != "OK56") return "fail 2"

            val b = bar<String, Int>()
            if (b != "OK56") return "fail 3"
            return "OK"
        }
    }
}
