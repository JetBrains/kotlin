// FILE: main.kt
fun some() {
    JavaClass.f<caret>ield;
}

// FILE: JavaClass.java
public class JavaClass {
    public static int field = 1;
}
