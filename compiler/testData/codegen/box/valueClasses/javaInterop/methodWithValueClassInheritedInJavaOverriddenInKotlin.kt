// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

// FILE: Point.kt

@JvmInline
value class Point(val x: Int, val y: Int)

// FILE: KotlinBase.kt

open class KotlinBase {
    open fun foo(x : Point) = 42
}

// FILE: JavaChild.java

public class JavaChild extends KotlinBase {}

// FILE: KotlinChild.kt

class KotlinChild : JavaChild() {
    override fun foo(x : Point) = 24
}

// FILE: box.kt

fun box(): String {
    if (KotlinBase().foo(Point(0, 0)) != 42) return "Fail 1"
    if (JavaChild().foo(Point(0, 0)) != 42) return "Fail 2"
    if (KotlinChild().foo(Point(0, 0)) != 24) return "Fail 3"

    return "OK"
}