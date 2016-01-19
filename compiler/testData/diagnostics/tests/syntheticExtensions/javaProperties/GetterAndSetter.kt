// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.something1++
    javaClass.something2++
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething1() { return 1; }
    public void setSomething1(int value) { }

    public int getSomething2() { return 1; }
    public JavaClass setSomething2(int value) { return this; }
}