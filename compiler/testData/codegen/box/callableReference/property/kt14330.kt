// IGNORE_BACKEND_FIR: JVM_IR
data class Foo(var bar: Int?)

fun box(): String {
    val receiver = Foo(1)
    Foo::bar.set(receiver, null)
    return if (receiver.bar == null) "OK" else "fail ${receiver.bar}"

}