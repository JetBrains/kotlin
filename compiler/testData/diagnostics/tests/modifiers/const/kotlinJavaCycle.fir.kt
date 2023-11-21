// IGNORE_REVERSED_RESOLVE
// IGNORE_CONTRACT_VIOLATIONS
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
        const val BAZ = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Bar.BAR + 1<!>
    }
}
