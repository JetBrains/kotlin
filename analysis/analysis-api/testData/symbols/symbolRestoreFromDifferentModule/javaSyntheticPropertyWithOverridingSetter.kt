// ISSUE: KT-91799
// MODULE: base
// FILE: Base.java
public class Base {
    public String getFoo() { return ""; }
    public void setFoo(String value) {}
}

// MODULE: derived(base)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public void setFoo(String value) {}
}

// MODULE: main(derived, base)
// FILE: main.kt
fun test(derived: Derived) {
    derived.f<caret>oo
}

// MODULE: restoreInContextOf(derived, base)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
