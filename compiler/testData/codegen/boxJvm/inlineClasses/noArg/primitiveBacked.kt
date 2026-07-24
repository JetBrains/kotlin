// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@JvmInline
value class Token(val value: Int)

class Holder(
    val token: Token = Token(83),
    val offset: Int = -4,
)

// FILE: Main.java
public class Main {
    public Holder create() {
        return new Holder();
    }
}

// FILE: box.kt
fun box(): String {
    val holder = Main().create()
    return (holder.token.value + holder.offset).toChar().toString() + "K"
}

