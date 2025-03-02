// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
object Foo {
    @JvmExposeBoxed
    @JvmStatic
    fun foo(): StringWrapper = StringWrapper("OK")
}

// FILE: Main.java
public class Main {
    public String test() {
        return Foo.foo().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}