// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib/JavaBox.java
package lib;

public class JavaBox {
    public static <T> java.util.List<T> wrap(java.util.List<T> list) {
        return list;
    }
}

// FILE: lib.kt
package lib

fun <T> wrap(list: List<T>) = JavaBox.wrap(list)

// MODULE: main(lib)
// FILE: main.kt
import lib.w<caret>rap
