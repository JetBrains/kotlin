// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

abstract class AbstractBase {
    abstract fun transform(id: Id): Id
}

// MODULE: main(lib)
// FILE: usage.kt
@file:OptIn(ExperimentalStdlibApi::class)

// The base comes from a separately compiled module, so the override is exposed against metadata rather than
// against IR present in the same compilation.
class Derived : AbstractBase() {
    @JvmExposeBoxed
    override fun transform(id: Id): Id = Id(id.value + "K")
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Derived().transform(new Id("O")).getValue();
    }
}

// FILE: box.kt
fun box(): String = Main().test()
