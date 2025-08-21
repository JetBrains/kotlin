// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Foo {
    @JvmExposeBoxed
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): String = sw.s

    @JvmExposeBoxed("foo22")
    @JvmName("foo21")
    fun foo2(): StringWrapper = StringWrapper("OK")
}

// FILE: Main.java
public class Main {
    public String test1() {
        return new Foo().foo11(new StringWrapper("OK"));
    }

    public String test2() {
        return new Foo().foo22().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    if (Main().test1() != "OK") return "FAIL 1 ${Main().test1()}"
    if (Main().test2() != "OK") return "FAIL 2 ${Main().test2()}"
    return "OK"
}