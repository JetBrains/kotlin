// FILE: main.kt
import dependency.JavaEnum

/**
 * [JavaEnum.myVal<caret>ues]
 */
fun test() {}

// FILE: dependency/JavaEnum.java
package dependency;

public enum JavaEnum {
    ENTRY;

    public static void myValues() {}
}