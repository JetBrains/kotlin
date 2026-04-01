// setter: callable: /C.foo

// FILE: main.kt
interface A { var foo: Int }

class C : B() {
    override var foo: Int
        get() = 0
        set(value) {}
}

// FILE: B.java
public class B implements A {
    public int getFoo() { return 0; }
    public void setFoo(int foo) {}
}
