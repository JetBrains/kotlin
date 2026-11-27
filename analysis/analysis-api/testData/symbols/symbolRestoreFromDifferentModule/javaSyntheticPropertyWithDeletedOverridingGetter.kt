// ISSUE: KT-91797
// 'Derived' of 'after' has no 'getFoo', so the restore in its context shows the pointer after the method is deleted
// MODULE: base
// FILE: Base.java
public class Base {
    public String getFoo() { return ""; }
    public void setFoo(String value) {}
}

// MODULE: before(base)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public String getFoo() { return ""; }
}

// MODULE: main(before, base)
// FILE: main.kt
fun test(derived: Derived) {
    derived.f<caret>oo
}

// MODULE: after(base)
// FILE: Derived.java
public class Derived extends Base {
}

// MODULE: restoreInContextOf(after, base)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
