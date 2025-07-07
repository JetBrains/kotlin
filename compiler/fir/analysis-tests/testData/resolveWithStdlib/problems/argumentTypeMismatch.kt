// RUN_PIPELINE_TILL: FRONTEND
// FILE: Sample.java

import java.util.List;

public class Sample {
    public static void foo(List<List<String>> listOfLists) {}
}

// FILE: test.kt

fun main() {
    Sample.foo(<!ARGUMENT_TYPE_MISMATCH("(Mutable)List<(Mutable)List<String!>!>!; String")!>"123"<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, stringLiteral */
