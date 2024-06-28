// ISSUE: KT-63070
// FILE: a/b.java
package a;

public class b {
    public void a_b() {}
}

// FILE:some/a.java
package some;

public class a {
    public static class b {
        public void some_ab() {}
    }
}

// FILE: c1.kt
package other

class a {}

fun test(a_: a.<!UNRESOLVED_REFERENCE!>b<!>) {
    val a_2 = a.<!UNRESOLVED_REFERENCE!>b<!>()
}

//FILE: c2.kt
package other2

class a {
    class b {
        fun other2_ab() {}
    }
}

fun test(_ab: a.b) {
    _ab.other2_ab()

    val _ab2 = a.b()
    _ab2.other2_ab()
}

// FILE: c3.kt
package some

fun test(_ab: a.b) {
    _ab.some_ab()

    val _ab2 = a.b()
    _ab2.some_ab()
}

// FILE: c4.kt
package a

fun test(_b: b) {
    _b.a_b()
}
