// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java
import java.util.*

public class J {
    public static <E extends Enum<E>> String foo(E e) { return ""; }
    public static <E extends Enum<E>> String foo(E e1, E e2) { return ""; }
    public static <E extends Enum<E>> String foo(E s1, E s2, E s3) { return ""; }
    public static <E extends Enum<E>> int foo(E... ss) { return 0; }
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
