// ISSUE: KT-39048

// FILE: JavaClass.java
public class JavaClass<T extends Comparable<? super T>> {
    public JavaClass(T from) {}
}

// FILE: main.kt

class K<T : Comparable<T>>(t: T)
fun main() {
    K(0)
    JavaClass(0)
}
