// FILE: main.kt
import dependency.JavaEnum

/**
 * [JavaEnum.EN<caret>TRY]
 */
fun test() {}

// FILE: dependency/JavaEnum.java
package dependency;

public enum JavaEnum {
    ENTRY
}