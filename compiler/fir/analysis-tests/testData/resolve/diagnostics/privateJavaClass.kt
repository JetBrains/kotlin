// ISSUE: KT-70157
// FILE: Some.java

public class Some {
    public static class Derived extends Base {}

    private static class Base {}
}

// FILE: test.kt

fun main() {
    val d = Some.Derived()
}
