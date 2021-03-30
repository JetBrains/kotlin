// FILE: KotlinFile.kt
abstract class KotlinClass : JavaInterface3 {
    override fun getSomething(): String = ""
}

fun foo(k: KotlinClass) {
    useString(k.getSomething())
    useString(k.something)
    if (<!SENSELESS_COMPARISON!>k.something == null<!>) return

    k.setSomething("")
    k.something = ""
}

fun useString(i: String) {}

// FILE: JavaInterface1.java
public interface JavaInterface1 {
    String getSomething();
}

// FILE: JavaInterface2.java
public interface JavaInterface2 {
    String getSomething();
    void setSomething(String value);
}

// FILE: JavaInterface3.java
public interface JavaInterface3 extends JavaInterface1, JavaInterface2 {}
