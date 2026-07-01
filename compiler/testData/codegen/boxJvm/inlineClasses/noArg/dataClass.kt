// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@JvmInline
value class Token(val value: String)

data class Holder(
    val token: Token = Token("O"),
    val text: String = "K",
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
    return holder.token.value + holder.text
}

