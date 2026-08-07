// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

// TODO: Review

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// Delegating a mutable collection over a value class element type generates a forwarder per member; each one
// exists in a mangled form plus an erased bridge, and the class-level annotation adds the boxed form beside
// both. 'MutableCollection' is covered by 'directive/kt87520.kt'.
@JvmExposeBoxed
class ListHolder(delegate: MutableList<Id>) : MutableList<Id> by delegate

@JvmExposeBoxed("newListHolder")
fun makeListHolder(): ListHolder = ListHolder(mutableListOf())

// FILE: Main.java
public class Main {
    public String test() {
        ListHolder holder = ICKt.newListHolder();
        holder.add(new Id("OK"));
        if (holder.indexOf(new Id("OK")) != 0) return "bad indexOf";
        if (!holder.contains(new Id("OK"))) return "bad contains";
        return holder.get(0).getValue();
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
