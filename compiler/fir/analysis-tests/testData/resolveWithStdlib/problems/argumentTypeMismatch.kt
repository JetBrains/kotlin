// RUN_PIPELINE_TILL: FRONTEND
// FILE: Sample.java

import java.util.List;

public class Sample {
    public static void foo(List<List<String>> listOfLists) {}
}

// FILE: test.kt

fun main() {
    Sample.foo(<!ARGUMENT_TYPE_MISMATCH("String; (Mutable)List<(Mutable)List<String!>!>!")!>"123"<!>)
}
