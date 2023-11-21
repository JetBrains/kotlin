// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: Bar.java
package one.two;

public class Bar {
    public static final int BAR1 = SOME_WRONG_EXPRESSION;
    public static final int BAR2 = MainKt.FOO + 1;
    public static final int BAR3 = Doo.DOO + 1;
}

// FILE: Main.kt
package one.two

const val FOO = 1

class Doo {
    companion object {
        const val DOO = 1
    }
}

const val BAZ1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Bar.BAR1 + 1<!>
const val BAZ2 = Bar.BAR2 + 1
const val BAZ3 = Bar.BAR3 + 1
