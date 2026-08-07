// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@JvmInline
value class Box<T>(val value: T)

fun <T> roundTrip(b: Box<T>): Box<T> = b

// FILE: Main.java
public class Main {
    public String test() {
        return (String) ICKt.roundTrip(new Box<String>("OK")).getValue();
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
