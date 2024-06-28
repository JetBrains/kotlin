// FILE: KotlinFile.kt
open class KotlinClass1 : JavaClass1() {
    public fun getSomethingKotlin1(): Int = 1
}

class KotlinClass2 : JavaClass2() {
    public fun getSomethingKotlin2(): Int = 1
}

fun foo(k: KotlinClass2) {
    useInt(k.getSomething1())
    useInt(k.something1)
    useInt(k.getSomething2())
    useInt(k.something2)
    useInt(k.getSomethingKotlin1())
    useInt(k.getSomethingKotlin2())
    k.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN!>somethingKotlin1<!>
    k.<!UNRESOLVED_REFERENCE!>somethingKotlin2<!>
}

fun useInt(i: Int) {}

// FILE: JavaClass1.java
public class JavaClass1 {
    public int getSomething1() { return 1; }
}

// FILE: JavaClass2.java
public class JavaClass2 extends KotlinClass1 {
    public int getSomething2() { return 1; }
}
