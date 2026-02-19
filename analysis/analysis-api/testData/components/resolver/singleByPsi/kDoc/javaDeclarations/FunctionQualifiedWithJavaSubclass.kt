// FILE: main.kt
import dependency.JavaChild

/**
 * [JavaChild.<caret>myFun]
 */
fun test() {}

// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public void myFun() {}
}

// FILE: dependency/JavaChild.java
package dependency;

public class JavaChild extends JavaBase {
}