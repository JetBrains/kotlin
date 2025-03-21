// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String)

class Foo {
    // Because of @JvmName, no exposed method can be generated, since it is unmangled
    @JvmName("foo")
    fun thenamedoesnotmatter(): StringWrapper = StringWrapper("OK")
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Foo().foo();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}