// FILE: main.kt
fun some() {
    JavaClass().<caret>count()
}

// FILE: JavaClass.java
public class JavaClass {
    public native int count();
}
