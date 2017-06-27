// FILE: a/b.java
package a;

public class b {
    public void a_b() {}
}

// FILE: a.java
public class a {
    public static class b {
        public void _ab() {}
    }
}

// FILE: some/c1.java
package some;

public class c1 {
    public a.b test() { return null; }
}


// FILE: a/c2.java
package a;

public class c2 {
    public a.b test() { return null; }
}

// FILE: c3.java
public class c3 {
    public a.b test() { return null; }
}

// FILE: c1.kt
package some

fun test() {
    val a_b = c1().test()
    a_b.a_b()
}

// FILE: c2.kt
package a

fun test() {
    val a_b = c2().test()
    a_b.a_b()
}

// FILE: c3.kt
fun test() {
    val _ab = c3().test()
    _ab._ab()
}
