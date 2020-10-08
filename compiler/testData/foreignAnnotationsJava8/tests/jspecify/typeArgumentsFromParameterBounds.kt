// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import org.jspecify.annotations.*;

public class A<T extends @NotNull Object, E extends @Nullable Object, F extends @NullnessUnknown Object> {
}

// FILE: B.java

import org.jspecify.annotations.*;

@DefaultNullable
public class B {
    public void bar(A<String, String, String> a) {}
}

// FILE: C.java

import org.jspecify.annotations.*;

@DefaultNotNull
public class C {
    public void bar(A<String, String, String> a) {}
}

// FILE: D.java

import org.jspecify.annotations.*;

@DefaultNullnessUnknown
public class D {
    public void bar(A<String, String, String> a) {}
}

// FILE: main.kt

fun main(
    aNotNullNotNullNotNull: A<String, String, String>,
    aNotNullNotNullNull: A<String, String, String?>,
    aNotNullNullNotNull: A<String, String?, String>,
    aNotNullNullNull: A<String, String?, String?>,
    b: B, c: C, d: D
) {
    b.bar(<!TYPE_MISMATCH!>aNotNullNotNullNotNull<!>)
    b.bar(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.bar(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    b.bar(aNotNullNullNull)

    c.bar(aNotNullNotNullNotNull)
    c.bar(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    c.bar(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    c.bar(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    d.bar(<!TYPE_MISMATCH!>aNotNullNotNullNotNull<!>)
    d.bar(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    d.bar(aNotNullNullNotNull)
    d.bar(aNotNullNullNull)
}
