// ISSUE: KT-91799
// The setter class 'Middle' is a subclass of the getter class 'Base', but the property comes from the scope of 'Derived'
// FILE: main.kt
interface I {
    var foo: String
}

fun some(derived: Derived) {
    derived.f<caret>oo
}

// FILE: Base.java
public class Base {
    public String getFoo() { return ""; }
    public void setFoo(String value) {}
}

// FILE: Middle.java
public class Middle extends Base {
    @Override
    public void setFoo(String value) {}
}

// FILE: Derived.java
public class Derived extends Middle implements I {
}
