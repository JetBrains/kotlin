// ISSUE: KT-91797
// 'Derived' of 'after' has no 'COUNT', so the restore in its context shows the pointer after the field is deleted
// MODULE: base
// FILE: Base.java
public class Base {
    public static int COUNT;
}

// MODULE: before(base)
// FILE: Derived.java
public class Derived extends Base {
    public static int COUNT;
}

// MODULE: main(before, base)
// FILE: main.kt
fun test() {
    Derived.CO<caret>UNT
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
