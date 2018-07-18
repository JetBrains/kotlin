// FILE: javaSyntheticPropertyAccess.kt
fun test(j: J) {
    j.foo += 1
}

// FILE: J.java
public class J {
    private int foo = 42;

    public int getFoo() { return foo; }
    public void setFoo(int x) {}
}