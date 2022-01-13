// SCOPE_DUMP: C:foo;getFoo
// FILE: A.java
public abstract class A {
    public int getFoo() {
        return 42;
    }
}

// FILE: B.kt
interface B {
    val foo: Int
}

// FILE: C.java
public class C extends A implements B {}

// FILE: main.kt
class D : C()
