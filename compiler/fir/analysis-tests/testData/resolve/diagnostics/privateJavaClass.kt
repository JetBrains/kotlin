// ISSUE: KT-70157
// FILE: Some.java

public class Some {
    public static class Derived extends Base {}

    private static class Base {}
}

// FILE: test.kt

fun main() {
    val d = <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>Some.Derived()<!>
}
