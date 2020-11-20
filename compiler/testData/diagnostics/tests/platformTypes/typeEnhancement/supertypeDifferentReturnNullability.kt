// FIR_IDENTICAL
// FILE: A.java
import org.jetbrains.annotations.*;

public interface A {
    @Nullable
    String foo();
}

// FILE: B.java
import org.jetbrains.annotations.*;

public interface B {
    @NotNull
    String foo();
}

// FILE: C.kt

class C1 : A, B {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = ""
}

class C2 : A, B {
    override fun foo(): String = ""
}

interface I : A, B

class C3 : I {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = ""
}

class C4 : I {
    override fun foo(): String = ""
}
