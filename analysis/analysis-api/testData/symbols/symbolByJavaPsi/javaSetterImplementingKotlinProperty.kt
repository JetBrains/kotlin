// DO_NOT_CHECK_SYMBOL_RESTORE
// FILE: main.kt
interface A { var foo: Int }

fun test(b: B) {
    b.fo<caret>o = 1
}

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
    public void setFoo(int foo) {}
}
