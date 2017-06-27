// FILE: a/x.java
package a;

public class x {
    public static class S {
        public static class B {}
    }
}

// FILE: a/x1.java
package a;

public class x1 extends x.S {
    public B getB() { return null; }
}

// FILE: a/x2.java
package a;

public class x2<B> extends x.S {
    public B getB() { return null; }
}

// FILE: a/test.kt
package a

fun test1() = x1().getB()
fun test2() = x2.<!UNRESOLVED_REFERENCE!>B<!>()
fun test3() = x2<String>().getB()