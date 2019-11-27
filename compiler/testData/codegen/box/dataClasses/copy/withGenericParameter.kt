// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Foo<String>) {}

class Foo<T>(val a: T) { }

fun box() : String {
    val f1 = Foo("a")
    val f2 = Foo("b")
    val a = A(f1)
    val b = a.copy(f2)
    if (b.a.a == "b") {
        return "OK"
    }
    return "fail"
}
