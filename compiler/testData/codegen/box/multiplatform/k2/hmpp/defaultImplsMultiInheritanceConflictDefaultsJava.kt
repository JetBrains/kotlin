// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib-common

expect class A
expect class B

expect interface Base1 {
    open fun foo(x: A): String
}

expect interface Base2 {
    fun foo(x: B): String
}

abstract class Derived : Base1, Base2 {
    abstract override fun foo(x: A): String
}

fun useCommon(d: Derived, a: A, b: B, b1: Base1, b2: Base2): String {
    val r1 = d.foo(a)
    val r2 = b1.foo(a)
    val r3 = b2.foo(b)
    return "$r1$r2$r3"
}

// MODULE: lib-platform()()(lib-common)

// FILE: C.java
public class C {
    @Override
    public String toString() {
        return "C";
    }
}

// FILE: JBase1.java
public interface JBase1 {
    default String foo(C x) {
        return "JBase1";
    }
}

// FILE: platform.kt
actual typealias A = C
actual typealias B = C

actual interface Base1 : JBase1 {
    actual override fun foo(x: C): String =
        super<JBase1>.foo(x)
}

actual interface Base2 {
    actual fun foo(x: C): String
}

class Impl : Derived(), Base1, Base2 {
    override fun foo(x: C): String {
        return "Impl" + super<Base1>.foo(x)
    }
}

// MODULE: app-common(lib-common)

// MODULE: app-platform(lib-platform)()(app-common)

fun box(): String {
    val c = C()
    val impl = Impl()
    val fromCommon = useCommon(impl, c, c, impl, impl)

    return if (fromCommon == "ImplJBase1ImplJBase1ImplJBase1") {
        "OK"
    } else {
        "FAIL"
    }
}
