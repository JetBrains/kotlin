// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalVersionOverloading::class)

@JvmInline
value class Token(val value: String)

class Holder @JvmExposeBoxed constructor(
    val token: Token = Token("O"),
    @IntroducedAt(version = "3") val text: String = "K",
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
