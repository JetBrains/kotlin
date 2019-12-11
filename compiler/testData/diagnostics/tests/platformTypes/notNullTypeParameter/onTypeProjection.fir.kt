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
    a.bar()?.length
    a.bar()?.length
}
