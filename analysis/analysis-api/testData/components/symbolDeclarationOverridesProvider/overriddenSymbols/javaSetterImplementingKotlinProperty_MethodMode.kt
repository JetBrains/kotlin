// callable: /B.setFoo

// FILE: main.kt
interface A { var foo: Int }

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
    public void setFoo(int foo) {}
}
