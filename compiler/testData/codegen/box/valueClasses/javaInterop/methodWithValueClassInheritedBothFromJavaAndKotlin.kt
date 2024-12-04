// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

// FILE: Point.kt

@JvmInline
value class Point(val x: Int, val y: Int)

// FILE: KotlinInterface.kt

public interface KotlinInterface {
    fun foo(x: Point): Int
}

// FILE: JavaInterface.java

public interface JavaInterface {
    int foo(Point x);
}

// FILE: JavaInterfaceChildOfKotlin.java

public interface JavaInterfaceChildOfKotlin extends KotlinInterface {}

// FILE: KotlinChild.kt

class KotlinChild : JavaInterface, JavaInterfaceChildOfKotlin {
    override fun foo(x: Point) = 42
}

// FILE: box.kt

fun box(): String {
    if (KotlinChild().foo(Point(0, 0)) != 42) return "Fail"
    return "OK"
}