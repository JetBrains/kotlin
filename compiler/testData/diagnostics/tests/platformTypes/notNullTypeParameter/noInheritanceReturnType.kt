// FIR_IDENTICAL
// FILE: A.java

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A<T> {
    public static A<String> create() {
        return null;
    }

    @NotNull
    public T bar() {
    }
}

// FILE: k.kt

fun test() {
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>A.create().bar()<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>A<String?>().bar()<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
}
