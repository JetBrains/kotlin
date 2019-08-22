// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import org.jspecify.annotations.*;

public class A<T extends @NotNull Object> {
    public void foo(T t) {}
    public <E extends @NotNull Object> void bar(E e) {}
}

// FILE: B.java

import org.jspecify.annotations.*;

@DefaultNotNull
public class B<T> {
    public void foo(T t) {}
    public <E> void bar(E e) {}
}

// FILE: main.kt

fun main(a1: A<<!UPPER_BOUND_VIOLATED!>Any?<!>>, a2: A<String>, b1: B<<!UPPER_BOUND_VIOLATED!>Any?<!>>, b2: B<String>) {
    a1.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar<String>("")

    a2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<String>("")

    b1.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b1.bar<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b1.bar<String>("")

    b2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b2.bar<<!UPPER_BOUND_VIOLATED!>String?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b2.bar<String>("")
}
