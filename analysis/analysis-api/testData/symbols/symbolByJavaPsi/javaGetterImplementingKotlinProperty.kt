// DO_NOT_CHECK_SYMBOL_RESTORE
// FILE: main.kt
interface A { val foo: Int }

fun test(b: B) {
    b.fo<caret>o
}

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
}
