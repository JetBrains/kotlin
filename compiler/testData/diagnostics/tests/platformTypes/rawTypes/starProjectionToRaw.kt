// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A<T extends CharSequence> {}

// FILE: B.java

import java.util.*;

public class B<E extends A> {}

// FILE: Test.java

class Test {
    static void foo(B x) {}
}

// FILE: main.kt


fun main(x: B<*>) {
    Test.foo(x)
}
