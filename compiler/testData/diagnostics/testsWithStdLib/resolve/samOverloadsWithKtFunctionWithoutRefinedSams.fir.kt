// !LANGUAGE: -RefinedSamAdaptersPriority -NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// !WITH_NEW_INFERENCE

// FILE: Foo.java
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class Foo {
    interface FObject {
        void invoke(Object i);
    }

    public String test(FObject f) { return ""; }
    public int test(Function1<Integer, Unit> f) { return 1; }
}

// FILE: 1.kt
fun bar() {
    Foo().<!AMBIGUITY!>test<!> {} <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
}
