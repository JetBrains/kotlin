// FILE: main.kt
import dependency.JavaClass

/**
 * [JavaClass.st<caret>aticField]
 */
fun test() {}

// FILE: dependency/JavaClass.java
package dependency;

public class JavaClass {
    public static final int staticField = 0;
}