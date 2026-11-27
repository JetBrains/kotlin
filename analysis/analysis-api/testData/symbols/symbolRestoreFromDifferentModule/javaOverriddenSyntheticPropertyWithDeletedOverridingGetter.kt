// ISSUE: KT-91797
// 'Derived' of 'after' has no 'getFoo', so the restore in its context shows the pointer after the method is deleted
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

// MODULE: before(base, i)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public int getFoo() { return 1; }
}

// MODULE: main(before, base, i)
// FILE: main.kt
fun test(derived: Derived) {
    derived.f<caret>oo
}

// MODULE: after(base, i)
// FILE: Derived.java
public class Derived extends Base {
}

// MODULE: restoreInContextOf(after, base, i)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
