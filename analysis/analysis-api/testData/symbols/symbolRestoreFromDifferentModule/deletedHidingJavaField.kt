// ISSUE: KT-91797
// 'Derived' of 'after' has no 'count', so the restore in its context shows the pointer after the field is deleted
// MODULE: base
// FILE: Base.java
public class Base {
    public int count;
}

// MODULE: before(base)
// FILE: Derived.java
public class Derived extends Base {
    public int count;
}

// MODULE: main(before, base)
// FILE: main.kt
fun test(derived: Derived) {
    derived.co<caret>unt
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
