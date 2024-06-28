// ISSUE: KT-63067
// FILE: KotlinFile.kt
open class KotlinClass {
    public open fun getSomething1(): Int = 1

    public open fun setSomething2(value: Int) {}
}

fun foo(javaClass: JavaClass) {
    useInt(javaClass.getSomething1())
    useInt(javaClass.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN!>something1<!>)

    javaClass.setSomething2(javaClass.getSomething2() + 1)
    javaClass.something2 = javaClass.something2 + 1
}

fun useInt(i: Int) {}

// FILE: JavaClass.java
public class JavaClass extends KotlinClass implements JavaInterface {
    public int getSomething1() { return 1; }

    public int getSomething2() { return 1; }
    public void setSomething2(int value) {}
}

// FILE: JavaInterface.java
public class JavaInterface {
    int getSomething2();
}
