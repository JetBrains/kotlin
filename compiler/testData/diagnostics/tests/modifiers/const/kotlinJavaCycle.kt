// IGNORE_REVERSED_RESOLVE
// IGNORE_CONTRACT_VIOLATIONS
// Issue appears bacause of `FirCompileTimeConstantEvaluator` from Analysis API.
// Problem should be resolved when we will switch to compiler's FIR evaluator.
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
