// FILE: main.kt
fun some(j: <caret>JavaInterface) {
}

// FILE: JavaInterface.java
public interface JavaInterface {
    JavaInterface foo();
}
