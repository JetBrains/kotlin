// FILE: a/b/c.java
package a.b;

public class c {
    public void ab_c() {}
}

// FILE: a/b.java
package a;

public class b {
    public void a_b() {}

    public static class c {
        public void a_bc() {}
    }
}

// FILE: d/d.java
package d;

public class d {

    public a.b test() { return null; }
    public a.b.c test2() { return null; }

}

// FILE: c.kt

fun t() = d.d().test()
fun t2() = d.d().test2()

fun test(a_b: a.b) {
    a_b.a_b()

    val ab_c = t2()
    ab_c.<!UNRESOLVED_REFERENCE!>ab_c<!>()
    ab_c.a_bc()
}

fun test2() = t().a_b()
