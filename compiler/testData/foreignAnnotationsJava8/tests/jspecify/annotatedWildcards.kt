// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import jspecify.annotations.*;

public class A<T extends @NotNull Object, E extends @Nullable Object, F extends @NullnessUnknown Object> {
}

// FILE: B.java

import jspecify.annotations.*;

public class B {
    public void superAsIs(A<? super CharSequence, ? super CharSequence, ? super CharSequence> a) {}
    public void superNotNull(A<? super @NotNull CharSequence, ? super @NotNull CharSequence, ? super @NotNull CharSequence> a) {}
    public void superNullable(A<? super @Nullable CharSequence, ? super @Nullable CharSequence, ? super @Nullable CharSequence> a) {}

    public void extendsAsIs(A<? extends CharSequence, ? extends CharSequence, ? extends CharSequence> a) {}
    public void extendsNotNull(A<? extends @NotNull CharSequence, ? extends @NotNull CharSequence, ? extends @NotNull CharSequence> a) {}
    public void extendsNullable(A<? extends @Nullable CharSequence, ? extends @Nullable CharSequence, ? extends @Nullable CharSequence> a) {}

    public void noBounds(A<?, ?, ?> a) {}
}

// FILE: main.kt

fun main(
    aNotNullNotNullNotNull: A<String, String, String>,
    aNotNullNotNullNull: A<String, String, String?>,
    aNotNullNullNotNull: A<String, String?, String>,
    aNotNullNullNull: A<String, String?, String?>,

    aAnyNotNullNotNullNotNull: A<Any, Any, Any>,
    aAnyNotNullNotNullNull: A<Any, Any, Any?>,
    aAnyNotNullNullNotNull: A<Any, Any?, Any>,
    aAnyNotNullNullNull: A<Any, Any?, Any?>,
    b: B
) {
    b.superAsIs(<!TYPE_MISMATCH!>aAnyNotNullNotNullNotNull<!>)
    b.superAsIs(<!TYPE_MISMATCH!>aAnyNotNullNotNullNull<!>)
    b.superAsIs(aAnyNotNullNullNotNull)
    b.superAsIs(aAnyNotNullNullNull)

    b.superNotNull(aAnyNotNullNotNullNotNull)
    b.superNotNull(aAnyNotNullNotNullNull)
    b.superNotNull(aAnyNotNullNullNotNull)
    b.superNotNull(aAnyNotNullNullNull)

    // TODO: Bound for the first argument in "superNullable" contradicts to declared nullability of the parameter
    // Do we need to ignore such arguments' nullability?
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNotNullNotNull<!>)
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNotNullNull<!>)
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNullNotNull<!>)
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNullNull<!>)

    b.extendsAsIs(aNotNullNotNullNotNull)
    b.extendsAsIs(aNotNullNotNullNull)
    b.extendsAsIs(aNotNullNullNotNull)
    b.extendsAsIs(aNotNullNullNull)

    b.extendsNotNull(aNotNullNotNullNotNull)
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.extendsNullable(aNotNullNotNullNotNull)
    b.extendsNullable(aNotNullNotNullNull)
    b.extendsNullable(aNotNullNullNotNull)
    b.extendsNullable(aNotNullNullNull)

    b.noBounds(aNotNullNotNullNotNull)
    b.noBounds(aNotNullNotNullNull)
    b.noBounds(aNotNullNullNotNull)
    b.noBounds(aNotNullNullNull)
}
