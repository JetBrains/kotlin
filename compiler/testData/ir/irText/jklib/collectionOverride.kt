// TARGET_BACKEND: JVM

// FILE: JavaClass.java
public class JavaClass {
    protected int myField;
}

// FILE: main.kt
abstract class MyKotlinClass : JavaClass() {
}
