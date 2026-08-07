// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// 'Nothing' is erased to 'java.lang.Void' in the JVM signature; the boxed variant must still unbox its
// parameter and reach the implementation.
@JvmExposeBoxed
fun failWith(id: Id): Nothing = throw IllegalStateException(id.value)

// FILE: Main.java
public class Main {
    public String test() {
        try {
            ICKt.failWith(new Id("OK"));
            return "no exception";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
