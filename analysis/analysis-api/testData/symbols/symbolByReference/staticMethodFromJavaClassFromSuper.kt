// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: BaseClass.java
public class BaseClass {
    public static void foo() {}
}

// FILE: Child.java
public class Child extends BaseClass {

}

// FILE: main.kt
package another

import Child.fo<caret>o
