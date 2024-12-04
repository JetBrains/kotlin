// FILE: main.kt
import dependency.JavaClass

/**
 * [JavaClass.st<caret>aticFun]
 */
fun test() {}

// FILE: dependency/JavaClass.java
package dependency;

public class JavaClass {
    public static void staticFun() {}
}