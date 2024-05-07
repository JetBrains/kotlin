// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: BaseClass.java
public class BaseClass {
    public static int bar = 1;
}

// FILE: Child.java
public class Child extends BaseClass {

}

// FILE: main.kt
package another

import Child.ba<caret>r
