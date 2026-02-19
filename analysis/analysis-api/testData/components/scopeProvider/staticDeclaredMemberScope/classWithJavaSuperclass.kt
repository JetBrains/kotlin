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

class KotlinClass : JavaClass() {
    fun foo(): Int = 5

    val bar: String = ""

    class C2

    object O2

    companion object {
        val baz: String = ""
    }
}
