// FILE: a/b/c.java
package a.b;

public class c {
    public void ab_c() {}
}

// FILE: a/b.java
package a;

public class b<T> {
    public static class c {
        public void a_bc() {}
    }
}

// FILE: c/d.java
package c;

import a.b.c;

public class d {

    public c getC() { return null; }

}

// FILE: c.kt
import a.b.c
import c.d

fun test(ab_c: c) {
    ab_c.ab_c()

    val ab_c2: a.b.c = a.b.c()
    ab_c2.ab_c()

    val ab_c3 = a.b.c()
    ab_c3.ab_c()
}

fun test2(ab_c: a.b.c) {
    ab_c.<!UNRESOLVED_REFERENCE!>a_bc<!>()
    ab_c.ab_c()
}

fun test3() = d().getC()

fun test4() {
    val ab_c = test3()
    ab_c.ab_c()

    val ab_c2 = test3()
    ab_c2.<!UNRESOLVED_REFERENCE!>a_bc<!>()
}
