// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Unexpected IR element found during code generation. Either code generation for it is not implemented, or it should have been lowered: ERROR_CALL 'Unresolved reference: <Unresolved name: getValue>#' type=IrErrorType([Error type: Unresolved type for getValue])
// MODULE: lib

// FILE: test/Foo.java
package test;

class Foo<T> {
    public static <K> String getValue(K key) {
        return null;
    }
}

// FILE: test/Bar.java
package test;

public class Bar extends Foo<String> {}

// MODULE: main(lib)
// FILE: k.kt
import test.Bar

fun test() {
    <!DEBUG_INFO_CALLABLE_OWNER("test.Foo.getValue in test.Foo")!>Bar.getValue("bar")<!>
}
