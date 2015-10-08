// FILE: A.java

public class A extends B {
    public int getFoo() { return 0; }
    public void setFoo(int x) {}
}

// FILE: F.java

public class F extends B {
    public final int getFoo() { return 0; }
    public final void setFoo(int x) {}
}

// FILE: ConflictingModality.java

public class ConflictingModality extends B {
    public final int getFoo() { return 0; }
    public abstract void setFoo(int x) {}
}

// FILE: ConflictingVisibility.java

public class ConflictingVisibility extends B {
    public int getFoo() { return 0; }
    protected void setFoo(int x) {}
}

// FILE: main.kt

open class B {
    // check that final is not overridden
    open protected var foo: Int = 1
}

class C1 : A() {
    override var foo: Int = 2
}

class C2 : F() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> var foo: Int = 3
}
