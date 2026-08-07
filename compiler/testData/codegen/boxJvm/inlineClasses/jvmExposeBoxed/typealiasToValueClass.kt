// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

typealias Alias = Id

// The typealias is expanded before the signature is mangled, so exposing a declaration written against the
// alias must behave exactly like exposing one written against the value class itself.
@JvmExposeBoxed
fun throughAlias(id: Alias): String = id.value

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.throughAlias(new Id("OK"));
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
