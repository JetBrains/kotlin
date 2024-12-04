// FILE: main.kt
import dependency.JavaBase

class KotlinChild : JavaBase() {
    /**
     * [st<caret>aticFun]
     */
    fun test() {}
}


// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public static void staticFun() {}
}
