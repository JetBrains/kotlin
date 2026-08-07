// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// The sealed base is abstract, so only the final leaf can be exposed.
sealed class Node(val id: Id) {
    fun describe(): String = id.value
}

class Leaf @JvmExposeBoxed constructor(id: Id) : Node(id)

// FILE: Main.java
public class Main {
    public String test() {
        return new Leaf(new Id("OK")).describe();
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
