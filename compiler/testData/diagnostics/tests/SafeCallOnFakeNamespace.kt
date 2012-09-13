// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: test.kt
fun ff() {
    val a = Test?.FOO
    System?.out.println(a)
}
