// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline class Ucn(private val i: UInt)

interface Input<T> {
    fun foo(n: Int = 0): T
}

fun Char.toUInt() = toInt().toUInt()

class Kx(val x: UInt) : Input<Ucn> {
    override fun foo(n: Int): Ucn =
        if (n < 0) Ucn(0u) else Ucn(x)
}

fun box(): String {
    val p = Kx(42u).foo()
    if (p.toString() != "Ucn(i=42)") throw AssertionError()

    return "OK"
}
