// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

interface Transformer {
    fun transform(id: Id): Id
}

class Delegate : Transformer {
    var calls: Int = 0

    override fun transform(id: Id): Id {
        calls++
        return Id(id.value + "K")
    }
}

// The generated forwarder is a final member of a final class, so the class-level annotation reaches it. The
// boxed variant has to forward to the delegate rather than duplicate its body.
@JvmExposeBoxed
class Forwarder(private val delegate: Delegate) : Transformer by delegate

// FILE: Main.java
public class Main {
    public String test(Delegate delegate) {
        return new Forwarder(delegate).transform(new Id("O")).getValue();
    }
}

// FILE: Box.kt
fun box(): String {
    val delegate = Delegate()
    val res = Main().test(delegate)
    if (res != "OK") return "FAIL 1: $res"
    if (delegate.calls != 1) return "FAIL 2: the boxed forwarder did not reach the delegate, calls=${delegate.calls}"
    return "OK"
}
