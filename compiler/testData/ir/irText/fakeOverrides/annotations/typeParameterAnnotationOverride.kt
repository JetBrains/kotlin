// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2

// FILE: Java1.java
import org.jetbrains.annotations.NotNull;

public class Java1<@NotNull T> {
    public T a;
    public T bar() {
        return a;
    }
    public void foo(T s) {}
}

// FILE: Java2.java
public class Java2 extends Java1<Integer> { }

// FILE: Java3.java
import org.jetbrains.annotations.Nullable;

public class Java3<@Nullable T> {
    public T a;
    public T bar() {
        return a;
    }
    public void foo(T s) {}
}

// FILE: Java4.java
import org.jetbrains.annotations.NotNull;
public class Java4<T extends @NotNull Number> {
    public T a;
    public T bar() {
        return a;
    }
    public void foo(T s) {}
}

// FILE: 1.kt
class A : Java1<Int>()

class B<T : Any> : Java1<T>()

class C : Java2()

class D : Java3<Int?>()

class E : Java3<Int>()

class F : Java4<Int>()

class G<T: Number> : Java4<T>()

fun test(a: A, b: B<String>, c: C, d: D, e: E, f: F, g: G<Int>) {
    val k: Int = a.a
    val k2: Int = a.bar()
    a.foo(1)

    val k3: String = b.a
    val k4: String = b.bar()
    b.foo("")

    val k5: Int = c.a
    val k6: Int = c.bar()
    c.foo(1)

    val k7: Int? = d.a
    val k8: Int? = d.bar()
    d.foo(null)

    val k9: Int = e.a
    val k10: Int = e.bar()
    e.foo(1)

    val k11: Int = f.a
    val k12: Int = f.bar()
    f.foo(1)

    val k13: Int = g.a
    val k14: Int = g.bar()
    g.foo(1)
}