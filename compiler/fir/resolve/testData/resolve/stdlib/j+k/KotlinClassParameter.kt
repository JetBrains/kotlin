// FILE: K1.kt
class KotlinClass

// FILE: JavaClass.java
public class JavaClass {
    public static void baz(KotlinClass k) {}
}

// FILE: K2.kt
fun main() {
    JavaClass.baz(KotlinClass())
    JavaClass.<!INAPPLICABLE_CANDIDATE!>baz<!>("")
}
