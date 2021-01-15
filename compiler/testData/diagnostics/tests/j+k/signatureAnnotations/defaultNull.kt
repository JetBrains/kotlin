// ANDROID_ANNOTATIONS
// FILE: A.java

import kotlin.annotations.jvm.internal.*;

public class A {
    public void foo(@DefaultNull Integer x) {}
    public void bar(@DefaultNull int x) {}
}

// FILE: B.java
import kotlin.annotations.jvm.internal.*;

public class B<T> {
    public void foo(@DefaultNull T t) { }
}

// FILE: test.kt
fun test(a: A, first: B<Int>, second: B<Int?>) {
    a.foo()
    a.foo(0)

    a.bar(<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.bar(0)

    first.foo()
    first.foo(5)

    second.foo()
    second.foo(5)
}
