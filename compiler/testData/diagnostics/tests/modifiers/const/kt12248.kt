// FIR_IDENTICAL
// FILE: Bar.java

public class Bar {
    public static final int BAR = Foo.FOO + 1;
}

// FILE: Test.kt

class Foo {
    companion object {
        const val FOO = 1
    }
}

class Baz {
    companion object {
        const val BAZ = Bar.BAR + 1
    }
}