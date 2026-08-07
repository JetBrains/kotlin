// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@JvmInline
value class Inner(val s: String)

@JvmInline
value class Outer(val inner: Inner)

fun unwrap(outer: Outer): String = outer.inner.s

// FILE: Main.java
public class Main {
    public String test() {
        return ICKt.unwrap(new Outer(new Inner("OK")));
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
