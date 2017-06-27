// FILE: a/b.java
package a;

public class b {
    
    public void a_b() {}

    public class c {
        public void a_bc() {}
    }    
}

// FILE: a.java

public class a {
    
    public void _a() {}

    public class b {
        public void _ab() {}
    }

}

// FILE: c.java

import a.b;

public class c {
    public b getB() { return null; }
}

// FILE: c2.java

public class c2 {
    public a.b getB() { return null; }
}

// FILE: e.kt

fun test() = c().getB().c().a_bc()
fun test2() = c2().getB()._ab()
