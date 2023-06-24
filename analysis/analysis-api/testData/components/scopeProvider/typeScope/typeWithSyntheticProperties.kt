// FILE: JavaClass.java
public class JavaClass {
    public Integer getValue() {
        return 1;
    }
}

// FILE: main.kt
class A : JavaClass()

fun test(a: A) {
    <expr>a</expr>
}