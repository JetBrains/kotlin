// FILE: main.kt
class <caret>C(delegate: J) : J by delegate

// FILE: J.java
public interface J {
    String foo();
}
