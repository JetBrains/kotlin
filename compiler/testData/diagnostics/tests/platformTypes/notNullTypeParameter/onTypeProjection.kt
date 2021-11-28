// FIR_IDENTICAL
// FILE: A.java

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A<T> {
    @NotNull
    public T bar() {
    }
}

// FILE: k.kt

fun test(a: A<out CharSequence>) {
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>a.bar()<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>a.bar()<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
}
