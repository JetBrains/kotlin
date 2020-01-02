// FILE: Outer.java

public abstract class Outer {
    protected static class My {}
    protected static class Your extends My {}
    abstract protected Your foo(My my);
}

// FILE: OuterDerived.kt

class OuterDerived: Outer() {
    // valid, My has better visibility
    protected class His: Outer.My()
    // valid, My and Your have better visibility
    override fun foo(my: Outer.My) = Outer.Your()
}