// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// !WITH_NEW_INFERENCE

// FILE: Foo.java
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class Foo {
    interface FObject<T> {
        void invoke(T i);
    }

    public String foo(FObject<Integer> f) { return ""; }
    public int foo(Function1<Integer, Unit> f) { return 1; }

    public String bar(FObject<Object> f) { return ""; }
    public int bar(Function1<Integer, Unit> f) { return 1; }
}

// FILE: 1.kt
fun test() {
    Foo().<!AMBIGUITY!>foo<!> {} <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
    Foo().<!AMBIGUITY!>bar<!> {} <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
}
