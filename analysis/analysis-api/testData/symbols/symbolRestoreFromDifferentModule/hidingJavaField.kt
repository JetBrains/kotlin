// ISSUE: KT-91797
// MODULE: base
// FILE: Base.java
public class Base {
    public int count;
}

// MODULE: derived(base)
// FILE: Derived.java
public class Derived extends Base {
    public int count;
}

// MODULE: main(derived, base)
// FILE: main.kt
fun test(derived: Derived) {
    derived.co<caret>unt
}

// MODULE: restoreInContextOf(derived, base)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
