// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java
import java.util.*;

public class J {
    public static <E1 extends Enum<E1>> String foo(E1 e) { return ""; }
    public static <E2 extends Enum<E2>> String foo(E2 e1, E2 e2) { return ""; }
    public static <E3 extends Enum<E3>> String foo(E3 s1, E3 s2, E3 s3) { return ""; }
    public static <E4 extends Enum<E4>> int foo(E4... ss) { return 0; }
}

// FILE: test.kt
enum class X { A }
val a = X.A

val test0: Int = J.foo<X>()
val test1: String = J.foo(a)
val test2: String = J.foo(a, a)
val test3: String = J.foo(a, a, a)
val test4: Int = J.foo(a, a, a, a)
val test5: Int = J.foo(a, a, a, a, a)
