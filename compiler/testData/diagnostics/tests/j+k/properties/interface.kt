// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT
// FILE: A.java

public interface A extends B {
    public int getFoo();
    public void setFoo(int x);
}

// FILE: BImpl.java

public class BImpl implements B {
    public int getFoo() {}
    public void setFoo(int x) {}
}

// FILE: main.kt

interface B {
    var foo: Int
}

interface C1 : A {
    override var foo: Int
}

class D : C1, BImpl()

fun foo() {
    BImpl().foo = BImpl().foo + 1
    D().foo = D().foo + 2
}


