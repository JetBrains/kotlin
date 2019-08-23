// FILE: K1.kt
class KotlinClass<T>

// FILE: JavaClass.java
public class JavaClass {
    public static void baz(KotlinClass<Integer> k) {}
}

// FILE: K2.kt
fun main() {
    JavaClass.baz(KotlinClass())
    JavaClass.baz(KotlinClass<Int>())
    JavaClass.baz(KotlinClass<String>())
    JavaClass.baz("")
}
