// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// FILE: Dependency.java
public class Dependency {
    public static int bar = 1;
}

// FILE: main.kt
package another

import Dependency.ba<caret>r
