// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: JavaBase.java

import kotlin.UInt;

public class JavaBase {
    public int foo(UInt x)
    {
        return 42;
    }
}

// FILE: JavaChild.java

public class JavaChild extends JavaBase {}

// FILE: KotlinChild.kt

class KotlinChild : JavaChild()

// FILE: box.kt

fun box(): String {
    if (JavaBase().foo(0.toUInt()) != 42) return "Fail 1"
    if (JavaChild().foo(0.toUInt()) != 42) return "Fail 2"
    if (KotlinChild().foo(0.toUInt()) != 42) return "Fail 3"

    return "OK"
}