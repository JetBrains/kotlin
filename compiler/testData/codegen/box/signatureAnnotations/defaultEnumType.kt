// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Signs.java
// ANDROID_ANNOTATIONS

public enum Signs {
    HELLO,
    WORLD;
}

// FILE: B.kt
enum class B {
    X,
    Y;
}

// FILE: A.java
import kotlin.annotations.jvm.internal.*;

class A {
    public Signs a(@DefaultValue("HELLO") Signs arg)  {
        return arg;
    }

    public B b(@DefaultValue("Y") B arg) {
        return arg;
    }
}

// FILE: test.kt
fun box(): String {
    val a = A()
    if (a.a() != Signs.HELLO) {
        return "FAIL: enums Java"
    }

    if (a.b() != B.Y) {
        return "FAIL: enums Kotlin"
    }

    return "OK"
}
