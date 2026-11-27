// ISSUE: KT-91797
// MODULE: i
// FILE: I.kt
interface I {
    val foo: Int
}

// MODULE: base(i)
// FILE: Base.java
public class Base implements I {
    public int getFoo() { return 0; }
}

// MODULE: derived(base, i)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public int getFoo() { return 1; }
}

// MODULE: main(derived, base, i)
// FILE: main.kt
fun test(derived: Derived) {
    derived.f<caret>oo
}

// MODULE: restoreInContextOf(derived, base, i)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
