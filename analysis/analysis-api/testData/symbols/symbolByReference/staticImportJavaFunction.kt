// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: Dependency.java
public class Dependency {
    public static void foo() {

    }
}

// FILE: main.kt
package another

import Dependency.fo<caret>o
