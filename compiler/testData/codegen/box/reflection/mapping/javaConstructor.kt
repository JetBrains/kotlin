// TARGET_BACKEND: JVM
// WITH_REFLECT
// MODULE: lib
// FILE: Java.java

public class Java {
    public Java(String s) {}
}

// MODULE: main(lib)
// FILE: 1.kt

import kotlin.reflect.*
import kotlin.reflect.jvm.*

class Kotlin(val x: Int)

@JvmInline
value class InlineClass(val y: UInt)

class WithInlineClass(val z: InlineClass)

class WithDefault(val d: Long = 0L)

fun box(): String {
    for (ctor in listOf(::Java, ::Kotlin, ::WithInlineClass, ::WithDefault)) {
        val java = ctor.javaConstructor ?: return "Fail: no Constructor for $ctor"
        val ctor2 = java.kotlinFunction
        if (ctor != ctor2) return "Fail: incorrect kotlinFunction for ctor=$ctor java=$java"
    }

    if (::InlineClass.javaConstructor != null)
        return "Fail: javaConstructor for inline class should be null because it's a method in the bytecode"

    return "OK"
}
