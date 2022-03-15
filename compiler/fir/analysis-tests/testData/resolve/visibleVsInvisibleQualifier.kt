// FILE: first/Some.java

package first;

public class Some {
    public static void foo() {}
}

// FILE: second/Some.java

package second;

/* package-private */ class Some {
    public static void bar() {}
}

// FILE: test/test.kt

package test

import first.*
import second.*

fun test() {
    Some.foo()
}
