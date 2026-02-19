// FILE: main.kt
import dependency.JavaChild

/**
 * [JavaChild.st<caret>aticFun]
 */
fun test() {}

// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public static void staticFun() {}
}

// FILE: dependency/JavaChild.java
package dependency;

public class JavaChild extends JavaBase {
}