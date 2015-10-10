// FILE: A.kt
open class A {
    open fun String.foo(y: String?): Int = 1
    open fun String?.bar(y: String): Int = 1
}

class E : B1() {
    fun baz() {
        val x: String? = ""

        x<!UNSAFE_CALL!>.<!>foo(x)
        x<!UNSAFE_CALL!>.<!>foo("")
        x.bar(<!TYPE_MISMATCH!>x<!>)
        x.bar("")
    }

    override fun String.foo(y: String?): Int = 1
    override fun String?.bar(y: String): Int = 1
}

// FILE: B.java
import org.jetbrains.annotations.*;

// Just inherit enhanced types
class B extends A {
    @Override
    int foo(String x, String y);
    @Override
    int bar(String x, String y);
}

// FILE: B1.java
import org.jetbrains.annotations.*;

// Just inherit enhanced types (annotations without conflicts)
public class B1 extends A {
    @Override
    public int foo(@NotNull String x, String y);
    @Override
    public int bar(@Nullable String x, String y);
}

// FILE: C.java
import org.jetbrains.annotations.*;

// Conflicting annotations. Everything is flexible
class C extends A {
    @Override
    int foo(@Nullable String x, @NotNull String y);
    @Override
    int bar(@NotNull String x, @Nullable String y);
}

// FILE: D.java
import org.jetbrains.annotations.*;

// Just inherit enhanced types (annotations without conflicts)
class D extends B {
    @Override
    int foo(@Nullable String x, @Nullable String y);
    @Override
    int bar(@NotNull String x, @NotNull String y);
}
