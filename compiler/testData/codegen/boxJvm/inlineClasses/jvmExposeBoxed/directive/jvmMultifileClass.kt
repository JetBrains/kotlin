// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: Meters.kt
@JvmInline
value class Meters(val v: String)

// FILE: one.kt
@file:JvmName("Multi")
@file:JvmMultifileClass

fun twice(m: Meters): Meters = Meters(m.v + m.v)

// FILE: two.kt
@file:JvmName("Multi")
@file:JvmMultifileClass

fun ok(m: Meters): String = m.v + "K"

// FILE: Main.java
public class Main {
    public String test() {
        return Multi.ok(Multi.twice(new Meters("O")));
    }
}

// FILE: Box.kt
fun box(): String {
    val java = Main().test()
    if (java != "OOK") return "FAIL Java: $java"
    return ok(Meters("O"))
}
