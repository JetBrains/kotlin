// ISSUE: KT-56616

// FILE: StubElement.java

public interface StubElement<T> {
    <E> E bar(E v);
}

// FILE: StubBasedPsiElement.java

public interface StubBasedPsiElement<T extends StubElement> {
    T foo1();

    StubElement foo2();
}

// FILE: test.kt

fun StubBasedPsiElement<*>.foo(): String? {
    if ("".hashCode() == 0) {
        return foo1().bar("")
    }

    return <!TYPE_MISMATCH!>foo2().<!TYPE_MISMATCH!>bar("")<!><!>
}
