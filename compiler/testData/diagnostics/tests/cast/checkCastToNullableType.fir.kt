// FILE: JavaClass.java

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class JavaClass {
    static Integer foo() {
        return 1;
    }

    @Nullable
    static Integer fooN() {
        return 1;
    }

    @NotNull
    static Integer fooNN() {
        return 1;
    }
}

// FILE: test.kt

fun <T, S: Any> test(x1: T, x2: T?, y1: S, y2: S?) {
    <!USELESS_IS_CHECK!>x1 is T?<!>
    <!USELESS_IS_CHECK!>x2 is T?<!>
    <!USELESS_IS_CHECK!>y1 is S?<!>
    <!USELESS_IS_CHECK!>y2 is S?<!>

    val f1 = JavaClass.foo()
    <!USELESS_IS_CHECK!>f1 is Int?<!>

    val f2 = JavaClass.fooN()
    <!USELESS_IS_CHECK!>f2 is Int?<!>

    val f3 = JavaClass.fooNN()
    <!USELESS_IS_CHECK!>f3 is Int?<!>
}
