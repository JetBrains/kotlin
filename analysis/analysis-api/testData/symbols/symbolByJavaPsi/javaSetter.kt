// FILE: main.kt
fun test(b: B) {
    b.fo<caret>o = 1
}

// FILE: B.java
public class B {
    public int getFoo() { return 0; }
    public void setFoo(int foo) {}
}
