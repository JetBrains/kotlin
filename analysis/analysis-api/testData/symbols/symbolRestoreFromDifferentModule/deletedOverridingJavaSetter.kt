// ISSUE: KT-91797
// SYMBOL_BY_JAVA_PSI
// 'Derived' of 'after' has no 'setFoo', so the restore in its context shows the pointer after the method is deleted
// 'Derived' overrides 'getFoo' too, as otherwise 'derived.foo' resolves to the property of 'Base' with 'Base.setFoo'
// MODULE: i
// FILE: I.kt
interface I {
    var foo: Int
}

// MODULE: base(i)
// FILE: Base.java
public class Base implements I {
    public int getFoo() { return 0; }
    public void setFoo(int value) {}
}

// MODULE: before(base, i)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public int getFoo() { return 1; }

    @Override
    public void setFoo(int value) {}
}

// MODULE: main(before, base, i)
// FILE: main.kt
fun test(derived: Derived) {
    derived.f<caret>oo = 1
}

// MODULE: after(base, i)
// FILE: Derived.java
public class Derived extends Base {
    @Override
    public int getFoo() { return 1; }
}

// MODULE: restoreInContextOf(after, base, i)
// FILE: restoreInContextOf.kt
fun restoreInContextOf() {
    <caret_restoreAt>
}
