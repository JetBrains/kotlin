// !LANGUAGE: -RefinedSamAdaptersPriority
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
    Foo().foo {} checkType { _<Int>() }
    Foo().bar {} checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Int>() }
}