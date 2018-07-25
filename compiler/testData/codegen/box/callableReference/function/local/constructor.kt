// IGNORE_BACKEND: JVM_IR
fun box(): String {
    class A {
        val result = "OK"
    }

    return (::A)().result
}
