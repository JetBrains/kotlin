// FILE: a/x.java
package a;

public class x {
    public static class y {
        public static final int I = 42;
    }
}

// FILE: b/x.java
package b;

public class x {
    public static final int I = a.x.y.I;
}