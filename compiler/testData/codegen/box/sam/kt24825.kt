// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: JavaClass.java
public class JavaClass {
    public static void run(Runnable l) {
        l.run();
    }

    public static void s(Runnable a, Runnable onError) {
        a.run();
    }
}

// FILE: main.kt

var state = ""

fun unit() {}

class B {
    val s = mutableListOf<Unit>()

    init {
        s.add(JavaClass.s({ state += "O" }, ::unit))

        Any().apply {
            JavaClass.run {
                s.add(JavaClass.s({ state += "K" }, ::unit))
            }
        }
    }
}

fun box(): String {
    B()
    return state
}
