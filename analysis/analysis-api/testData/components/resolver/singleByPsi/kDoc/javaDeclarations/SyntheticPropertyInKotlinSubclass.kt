// FILE: main.kt
import dependency.JavaBase

/**
 * [<caret_2>prop]
 * [KotlinChild.<caret_1>prop]
 */
class KotlinChild : JavaBase()

// FILE: dependency/JavaBase.java
package dependency;

public class JavaBase {
    public String getProp() { return null; }
}
