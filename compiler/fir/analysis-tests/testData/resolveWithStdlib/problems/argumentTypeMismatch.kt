// FILE: Sample.java

import java.util.List;

public class Sample {
    public static void foo(List<List<String>> listOfLists) {}
}

// FILE: test.kt

fun main() {
    Sample.foo(<!ARGUMENT_TYPE_MISMATCH("ft<kotlin/collections/MutableList<ft<kotlin/collections/MutableList<kotlin/String!>, kotlin/collections/List<kotlin/String!>?>>, kotlin/collections/List<ft<kotlin/collections/MutableList<kotlin/String!>, kotlin/collections/List<kotlin/String!>?>>?>; kotlin/String")!>"123"<!>)
}
