// class: test/KotlinClass
// FILE: JavaClass.java
package test

public abstract class JavaClass {
    public void perform() {
    }

    public int x = 0;

    public static void hello() {
    }

    public static int y = 1;

    public static class C1 {
    }
}

// FILE: KotlinClass.kt
package test

// When a java class inherits from `JavaClass`, its static member scope contains `hello` and `y`. However, when a Kotlin class inherits from
// `JavaClass`, like this class, its static member scope does not contain the Java super-class's static callables.
class KotlinClass : JavaClass() {
    fun foo(): Int = 5

    val bar: String = ""

    class C2

    object O2

    companion object {
        val baz: String = ""
    }
}
