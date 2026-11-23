// ISSUE: KT-92046
// 'Derived' of 'after' has no 'value', so the restore in its context shows the pointer after the field is deleted
// MODULE: base
// FILE: Base.java
public class Base<T> {
    public T value;
}

// MODULE: before(base)
// FILE: Derived.java
public class Derived extends Base<String> {
    public String value;
}

// MODULE: main(before, base)
// FILE: main.kt
fun test(derived: Derived) {
    derived.va<caret>lue
}

// MODULE: after(base)
// FILE: Derived.java
public class Derived extends Base<String> {
}

// MODULE: restoreInContextOf(after, base)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
