// FILE: main.kt
import dependency.JavaBase

class KotlinChild : JavaBase()

/**
 * [KotlinChild.<caret>myFun]
 */
fun test() {}

// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public void myFun() {}
}
