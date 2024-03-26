// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
import org.jetbrains.annotations.Nullable;

public class Java1 {
    @Nullable
    public String nullableString = "";
    @Nullable
    public String bar() {
        return nullableString;
    }
    public void foo(@Nullable String s) {}
}

// FILE: Java2.java
import org.jetbrains.annotations.NotNull;

public interface Java2  {
    @NotNull
    public String nullableString = "";
    @NotNull
    public String bar();
    public void foo(@NotNull String s);
}

// FILE: 1.kt

class A : Java1()

class B : Java1() {
    override fun foo(s: String?) { }
    override fun bar(): String {
        return null!!
    }
}

abstract class C: Java2

class D : Java2 {
    override fun bar(): String {
        return ""
    }
    override fun foo(s: String) { }
}


fun test(a:A, b: B, c: C, d: D) {
    val k: String? = a.nullableString
    val k2: String? = a.bar()
    a.foo(null)
    a.foo("")

    val k3: String? = b.nullableString
    val k4: String = b.bar()
    b.foo(null)
    b.foo("")

    val k5: String = c.bar()
    c.foo("")

    val k6: String = d.bar()
    d.foo("")
}