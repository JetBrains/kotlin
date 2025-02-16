// FILE: main.kt
fun JavaClass.foo(javaClass: JavaClass) {
    (<caret_1>something)++
    (<caret_2>something) = 1
    (javaClass.<caret_3>something) = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) {}
}
