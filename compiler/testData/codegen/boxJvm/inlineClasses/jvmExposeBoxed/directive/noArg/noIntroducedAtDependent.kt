// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: Test.kt
@JvmInline
value class Token(val value: String)

class Holder(
    val token: Token = Token("O"),
    val text: String = token.value + "K",
)

// FILE: Main.java
public class Main {
    public Holder create() {
        return new Holder();
    }
}

// FILE: box.kt
fun box(): String {
    return Main().create().text
}

