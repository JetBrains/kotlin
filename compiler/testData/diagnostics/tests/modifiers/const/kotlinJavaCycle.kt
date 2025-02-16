// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION: FIR2IR reports "Const field is not containing const expression", and FIR must result in a diagnostic, but does not...
// FIR_IDENTICAL
// FILE: Bar.java

public class Bar {
    public static final int BAR = Foo.FOO + 1;
}

// FILE: Test.kt

class Foo {
    companion object {
        const val FOO = Baz.BAZ + 1
    }
}

class Baz {
    companion object {
        const val BAZ = Bar.BAR + 1
    }
}
