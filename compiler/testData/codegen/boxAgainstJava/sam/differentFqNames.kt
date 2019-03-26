// WITH_RUNTIME
// FILE: Custom.java

class Custom {
    public interface Runnable {
        void run2();
    }
}

// FILE: 1.kt

fun box(): String {
    val f = { }
    val class1 = Runnable(f).javaClass
    val class2 = Custom.Runnable(f).javaClass

    return if (class1 != class2) "OK" else "Same class: $class1"
}
