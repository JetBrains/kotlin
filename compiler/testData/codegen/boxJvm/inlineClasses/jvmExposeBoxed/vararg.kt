// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// The mangling comes from the value class parameter, not from the vararg. A vararg *of* a value class is only
// expressible with an unsigned type - see 'varargUnsigned.kt'.
@JvmExposeBoxed
fun concat(vararg parts: String, id: Id): String = parts.joinToString("") + id.value

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.concat(new String[] { "O" }, new Id("K"));
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
