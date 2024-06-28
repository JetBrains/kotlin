// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public String nullableString = "";
    public String bar() {
        return nullableString;
    }
    public void foo(String s) {}
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

// FILE: Java3.java
import org.jetbrains.annotations.Nullable;

interface Java3 {
    @Nullable
    String nullableString = "";
    @Nullable
    String bar();
    void foo(@Nullable String s);
}

// FILE: 1.kt
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class A : Java1(), Java2 {
    override fun bar(): String {
        return ""
    }
    override fun foo(s: String) { }
}

class B : Java1(), Java2

class C : Java1(), Java3

class D : Java1(), Java3 {

    @Nullable
    override fun bar(): String {
        return super.bar()
    }
    @NotNull
    override fun foo(@NotNull s: String?) {
        super.foo(s)
    }
}

abstract class E: Java2, KotlinInterface

class F : Java2, KotlinInterface {
    override fun bar(): String {
        return ""
    }
    override fun foo(s: String) { }
}

class G : Java1(), KotlinInterface2

class H : Java1(), Java2, Java3

interface KotlinInterface {
    val nullableString: String
        get() = ""
    fun bar(): String
    fun foo(s: String)
}

interface KotlinInterface2 {
    @get:NotNull
    val nullableString: String
        get() = ""
    @NotNull
    fun bar(): String
    @NotNull
    fun foo(@NotNull s: String)
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) {
    val k: String? = a.nullableString
    val k2: String = a.bar()
    a.foo("")

    val k3: String = b.nullableString
    val k4: String = b.bar()
    b.foo("")
    b.foo(null)

    val k5: String = c.nullableString
    val k6: String? = c.bar()
    c.foo("")
    c.foo(null)

    val k7: String = d.nullableString
    val k8: String = d.bar()
    d.foo("")
    d.foo(null)

    val k9: String = e.nullableString
    val k10: String = e.bar()
    e.foo("")

    val k11: String = f.nullableString
    val k12: String = f.bar()
    f.foo("")

    val k13: String = g.nullableString
    val k14: String = g.bar()
    g.foo("")
    g.foo(null)

    val k15: String = h.nullableString
    val k16: String = h.bar()
    h.foo("")
    h.foo(null)
}