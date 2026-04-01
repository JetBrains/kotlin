// FILE: main.kt
fun test(b: B) {
    b.fo<caret>o
}

// FILE: B.java
public class B {
    public int getFoo() { return 0; }
}
