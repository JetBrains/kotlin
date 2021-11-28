// FILE: J.java
import org.jetbrains.annotations.NotNull;

public interface J {
    @NotNull
    public Integer foo();
}

// FILE: safeCallToPrimitiveEquality3.kt

fun doJava1(s: String?, j: J) = s?.length == j.foo()

fun doJava2(s: String?, j: J) = j.foo() == s?.length

// `doJava1`/`doJava2` box `s?.length` instead of unboxing `j.foo()`:
// 2 valueOf
