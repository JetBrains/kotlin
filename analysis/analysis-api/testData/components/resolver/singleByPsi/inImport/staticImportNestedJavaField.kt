// FILE: Dependency.java
public class Dependency {
    public static class Nested {
        public static int bar = 1;
    }
}

// FILE: main.kt
package another

import Dependency.Nested.ba<caret>r
