// callable: /B.getFoo

// FILE: main.kt
interface A { val foo: Int }

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
}
