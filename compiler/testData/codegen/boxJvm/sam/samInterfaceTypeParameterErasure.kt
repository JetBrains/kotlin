// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS
// ^ test checks reflection for synthetic classes

// FILE: S.java

import java.util.Arrays;

interface S<A extends Number, B extends A, C extends A, D extends Comparable<B>, E extends C> {
    void accept(A a, B b, C c, D d, E e);
}

// FILE: J.java

import java.util.Arrays;

class J {
    public static String foo(S<Number, Long, Integer, Comparable<Long>, Integer> s) {
        return Arrays.toString(s.getClass().getGenericInterfaces());
    }
}

// FILE: 1.kt

fun box(): String {
    val supertypes = J.foo { a, b, c, d, e -> }
    return if (supertypes == "[interface S]") "OK" else "Fail: $supertypes"
}
