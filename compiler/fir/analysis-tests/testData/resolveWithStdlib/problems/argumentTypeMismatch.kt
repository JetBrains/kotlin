// RUN_PIPELINE_TILL: FRONTEND
// FILE: Sample.java

import java.util.List;

public class Sample {
    public static void foo(List<List<String>> listOfLists) {}
}

// FILE: test.kt

fun main() {
    Sample.foo(<!ARGUMENT_TYPE_MISMATCH("kotlin.String; kotlin.collections.(Mutable)List<kotlin.collections.(Mutable)List<kotlin.String!>!>!")!>"123"<!>)
}
