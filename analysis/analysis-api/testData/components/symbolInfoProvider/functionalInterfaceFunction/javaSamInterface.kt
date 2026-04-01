// FILE: JavaRunnable.java
public interface JavaRunnable {
    void run();
}

// FILE: main.kt
fun usage(r: <caret>JavaRunnable) {}
