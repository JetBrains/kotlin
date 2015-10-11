// FILE: A.java

public class A extends B {
    public int isFoo() { return 0; }
    public void setFoo(int x) {}
}

// FILE: F.java

public class F extends B {
    public final int isFoo() { return 0; }
    public final void setFoo(int x) {}
}

// FILE: main.kt

open class B {
    open var isFoo: Int = 1
}

class C1 : A() {
    override var isFoo: Int = 2
}

class C2 : F() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> var isFoo: Int = 3
}
