// ISSUE: KT-57979

// FILE: J.java
import org.jetbrains.annotations.*;

public class J<T> {
    @Nullable
    public T getValue1() {
        return null;
    }

    public void setValue1(T value) {
    }

    @NotNull
    public T getValue2() {
        return null;
    }

    public void setValue2(T value) {
    }

    public T getValue3() {
        return null;
    }

    public void setValue3(T value) {
    }
}
// FILE: main.kt

fun test(j: J<Unit>) {
    j.value1 = Unit
    j.value2 = Unit
    j.value3 = Unit

    j.value1 = null
    j.value2 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    j.value3 = null

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>j.value1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>j.value2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit..kotlin.Unit?!")!>j.value3<!>
}
