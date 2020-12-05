// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=indy-with-constants
// JVM_TARGET: 9
// FILE: JavaClass.java

public class JavaClass {
    public String toString() {
        return null;
    }
}

// FILE: Kotlin.kt
fun box() {
    val toString: String? = JavaClass().toString()
    val template: String = "${JavaClass()}"
}
// 0 INVOKEDYNAMIC makeConcat
// 1 JavaClass.toString
// 1 String.valueOf
// 0 append