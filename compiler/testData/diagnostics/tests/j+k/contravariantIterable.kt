// FILE: A.java
import java.util.*;
public class A {
    public static void foo(Iterable<? super CharSequence> x) {}
    public static void bar(Iterator<? super CharSequence> x) {}
}

// FILE: main.kt

fun test(x: List<String>, y: List<*>, z: MutableList<*>, w: MutableList<in CharSequence>) {
    A.foo(x)
    A.foo(y)
    A.foo(z)
    A.foo(w)

    A.bar(x.iterator())
    A.bar(y.iterator())
    A.bar(z.iterator())
    A.bar(w.iterator())
}
