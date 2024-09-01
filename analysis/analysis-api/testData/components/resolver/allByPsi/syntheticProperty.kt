// WITH_STDLIB
// FILE: main.kt
fun JavaClass.foo(javaClass: JavaClass) {
    print(javaClass.something)
    javaClass.something = 1
    javaClass.something += 1
    javaClass.something++
    --javaClass.something

    something++
    (something)++
    (something) = 1
    (javaClass.something) = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) {}
}

// IGNORE_STABILITY_K2: candidates
// ^KT-69962