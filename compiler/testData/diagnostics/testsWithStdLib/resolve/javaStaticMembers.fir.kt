// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: test.kt
fun ff() {
    val a = Test.FOO
    val b = Test?.FOO
    System.out.println(a + b)
    System?.out.println(a + b)
}