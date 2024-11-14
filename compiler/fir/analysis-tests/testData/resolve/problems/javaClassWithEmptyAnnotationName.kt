// RUN_PIPELINE_TILL: BACKEND
// DISABLE_JAVA_FACADE
// FILE: main.kt

fun main() {
    val j: JavaClass =  JavaClass()
}

// FILE: JavaClass.java

@()
public class JavaClass {}