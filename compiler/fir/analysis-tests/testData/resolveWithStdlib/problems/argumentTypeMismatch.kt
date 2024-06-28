// FILE: Sample.java

import java.util.List;

public class Sample {
    public static void foo(List<List<String>> listOfLists) {}
}

// FILE: test.kt

fun main() {
    Sample.foo(<!ARGUMENT_TYPE_MISMATCH("kotlin.collections.(Mutable)List<kotlin.collections.(Mutable)List<kotlin.String!>!>!; kotlin.String")!>"123"<!>)
}
