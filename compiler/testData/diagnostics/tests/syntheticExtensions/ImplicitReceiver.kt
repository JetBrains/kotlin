// FILE: KotlinFile.kt
fun JavaClass.foo() {
    useInt(getSomething())
    useInt(something)
}

fun useInt(<!UNUSED_PARAMETER!>i<!>: Int) {}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
}