// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.something++
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) { }
}