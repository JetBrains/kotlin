// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Test.kt
@JvmInline
value class Token(val value: String)

class Holder(val token: Token) {
    constructor() : this(Token("OK"))
}

// FILE: Main.java
public class Main {
    public Holder create() {
        return new Holder();
    }
}

// FILE: box.kt
fun box(): String {
    return Main().create().token.value
}

