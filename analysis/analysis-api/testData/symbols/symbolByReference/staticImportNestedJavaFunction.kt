// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// FILE: Dependency.java
public class Dependency {
    public static class Nested {
        public static void foo()
        {

        }
    }
}

// FILE: main.kt
package another

import Dependency.Nested.fo<caret>o
