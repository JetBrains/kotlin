// FILE: A.java
import org.jetbrains.annotations.*;

public interface A {
    void foo(@Nullable String x);
}

// FILE: B.java
import org.jetbrains.annotations.*;

public interface B {
    void foo(@NotNull String x);
}

// FILE: C.kt

class C1 : A, B {
    override fun foo(x: String) {}
}

class C2 : A, B {
    override fun foo(x: String?) {}
}

interface I : A, B

class C3 : I {
    override fun foo(x: String) {}
}

class C4 : I {
    override fun foo(x: String?) {}
}