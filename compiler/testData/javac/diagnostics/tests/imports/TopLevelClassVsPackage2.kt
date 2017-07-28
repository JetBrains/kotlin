// FILE: foo/a/b.java
package foo.a;

public class b {

    public void a_b() {}

    public class c {
        public void a_bc() {}
    }
}

// FILE: foo/a.java
package foo;

public class a {

    public void _a() {}

    public class b {
        public void _ab() {}
    }

}

// FILE: foo/c.java
package foo;

import foo.a.b;

public class c {
    public b getB() { return null; }
}

// FILE: foo/c2.java
package foo;

public class c2 {
    public a.b getB() { return null; }
}

// FILE: e.kt
package foo

fun test() = c().getB().c().a_bc()
fun test2() = c2().getB()._ab()