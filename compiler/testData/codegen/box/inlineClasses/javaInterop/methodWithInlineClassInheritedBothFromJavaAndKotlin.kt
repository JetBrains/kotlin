// WITH_STDLIB
// TARGET_BACKEND: JVM_IR


// FILE: KotlinInterface.kt

public interface KotlinInterface {
    fun foo(x: UInt): Int
}

// FILE: JavaInterface.java

import kotlin.UInt;

public interface JavaInterface {
    int foo(UInt x);
}

// FILE: JavaInterfaceChildOfKotlin.java

public interface JavaInterfaceChildOfKotlin extends KotlinInterface {}

// FILE: KotlinChild.kt

class KotlinChild : JavaInterface, JavaInterfaceChildOfKotlin {
    override fun foo(x: UInt) = 42
}

// FILE: box.kt

fun box(): String {
    if (KotlinChild().foo(0.toUInt()) != 42) return "Fail"
    return "OK"
}