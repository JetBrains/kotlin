// FILE: A.java

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A<T> {
    public void bar(@NotNull T x) {
    }
}

// FILE: B1.java
public class B1 extends A<String> {
    // real override
    public void bar(String x) {
    }
}

// FILE: B2.java
public class B2 extends A<String> {
    // fake override bar
}

// FILE: k.kt

class C1 : A<String?>() {
    override fun bar(x: String) {}
}

class C2 : A<String?>() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(x: String?) {}
}

fun test() {
    B1().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    B2().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    C1().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

