// ANDROID_ANNOTATIONS
// FILE: A.java
import kotlin.annotations.jvm.internal.*;
import kotlin.internal.*;

public class A {
    public void dollarName(@ParameterName("$") String host) {
    }

    public void numberName(@ParameterName("42") String field) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.<!INAPPLICABLE_CANDIDATE!>dollarName<!>(`$` = "hello")
    test.dollarName("hello")
    test.dollarName(host = "hello")

    test.<!INAPPLICABLE_CANDIDATE!>numberName<!>(`42` = "world")
    test.numberName("world")
    test.numberName(field = "world")
}
