// WITH_RUNTIME
@JvmInline
value class Z(val x: Int)

var topLevel = Z(42)

fun box(): String {
    val ref = ::topLevel

    if (ref.get().x != 42) throw AssertionError()

    ref.set(Z(1234))
    if (ref.get().x != 1234) throw AssertionError()

    return "OK"
}