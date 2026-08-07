// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Box<T>(val value: T)

@JvmExposeBoxed
fun <T> roundTrip(b: Box<T>): Box<T> = b

// FILE: Main.java
public class Main {
    public String test() {
        return (String) ICKt.roundTrip(new Box<String>("OK")).getValue();
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
