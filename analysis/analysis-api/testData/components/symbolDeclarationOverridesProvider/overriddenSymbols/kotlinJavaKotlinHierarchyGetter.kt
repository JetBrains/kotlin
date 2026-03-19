// getter: callable: /C.foo

// FILE: main.kt
interface A { val foo: Int }

class C : B() {
    override val foo: Int get() = 0
}

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
}
