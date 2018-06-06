// FILE: a/x.java
package a;

public class x {

    public b getB() { return null; }

    public static class b {

        public b getB() { return null; }

        public static class b {

            public b getB() { return null; }

            public static class b {
                public b getB() { return null; }
            }

        }

    }

}

// FILE: b/x.java
package b;

import a.x.b.b.b;

public class x {
    public b getB() { return null; }
}

// FILE: b/y.java
package b;

import a.x.b.b.*;

public class y {
    public b getB() { return null; }
}

// FILE: b/test.kt
package b

fun test() = x().getB()
fun test2() = y().getB()