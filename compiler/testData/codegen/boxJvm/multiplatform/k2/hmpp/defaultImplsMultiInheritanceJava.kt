// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib-common

expect class A
expect class B

expect interface Base1 {
    open fun foo(x: A): String
}

expect interface Base2 {
    open fun foo(x: B): String
}

interface I1 : Base1
interface I2 : Base2

abstract class Derived : I1, I2 {
    abstract override fun foo(x: A): String
}

fun useCommon(d: Derived, a: A, b: B, d1: I1, d2: I2): String {
    val r1 = d.foo(a)
    val r2 = d1.foo(a)
    val r3 = d2.foo(b)
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

// FILE: JBase2.java
public interface JBase2 {
    default String foo(C x) {
        return "JBase2";
    }
}

// FILE: platform.kt
actual typealias A = C
actual typealias B = C

actual interface Base1 : JBase1 {
    actual override fun foo(x: C): String =
        super<JBase1>.foo(x)
}

actual interface Base2 : JBase2 {
    actual override fun foo(x: C): String =
        super<JBase2>.foo(x)
}

class Impl : Derived(), I1, I2 {
    override fun foo(x: C): String {
        return "Impl" + super<I1>.foo(x)
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
