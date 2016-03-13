// FILE: A.java

public interface A extends A.B {
    interface B extends A { public int getFoo() { return 1; } }
}

// FILE: A0.java
public interface A0 extends A0.B {
    interface B { public int getFoo() { return 1; } }
}

// FILE: B.java

public class B extends D {
    public int getFoo() { return 1; }
    public static class C {
        public int getFoo() { return 1; }
    }
}

// FILE: D.java
public class D extends B.C {
    public int getFoo() { return 1; }
}
