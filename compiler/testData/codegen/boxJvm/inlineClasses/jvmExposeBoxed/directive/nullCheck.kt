// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class IntWrapper(val i: Int)

fun f(p: IntWrapper): Int = p.i

// FILE: Main.java
public class Main {
    public String test() {
        try {
            ICKt.f(null);
        } catch (NullPointerException e) {
            return e.getMessage();
        }
        return "no exception";
    }
}

// FILE: Box.kt
fun box(): String {
    val message = Main().test()
    if (!message.startsWith("Parameter specified as non-null is null")) return "FAIL: $message"
    return "OK"
}
