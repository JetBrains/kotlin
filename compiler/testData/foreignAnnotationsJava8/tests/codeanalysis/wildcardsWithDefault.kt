// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import jspecify.annotations.*;

public class A<T extends @NotNull Object, E extends @Nullable Object, F extends @UnknownNullness Object> {
}

// FILE: B.java

import jspecify.annotations.*;

public class B {
    @DefaultNotNull
    public void noBoundsNotNull(A<?, ?, ?> a) {}
    @DefaultNullable
    public void noBoundsNullable(A<?, ?, ?> a) {}
}

// FILE: main.kt

fun main(
    aNotNullNotNullNotNull: A<String, String, String>,
    aNotNullNotNullNull: A<String, String, String?>,
    aNotNullNullNotNull: A<String, String?, String>,
    aNotNullNullNull: A<String, String?, String?>,
    b: B
) {
    b.noBoundsNotNull(aNotNullNotNullNotNull)
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}
