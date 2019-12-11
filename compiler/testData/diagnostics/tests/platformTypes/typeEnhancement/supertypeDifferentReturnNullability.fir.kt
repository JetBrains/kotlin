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
    override fun foo(): String? = ""
}

class C2 : A, B {
    override fun foo(): String = ""
}

interface I : A, B

class C3 : I {
    override fun foo(): String? = ""
}

class C4 : I {
    override fun foo(): String = ""
}
