// IGNORE_FE10
// KT-70384

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: Common.java
package library;

public class Common {
    public String foo() {
        return "common";
    }
}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: Common.java
package library;

public class Common {
    public int bar() {
        return 5;
    }
}

// MODULE: module1(library1)
// FILE: main1.kt
import library.Common

fun test() {
    Common().f<caret>oo()
}

// MODULE: module2(library2)
// FILE: main2.kt
import library.Common

fun test() {
    Common().b<caret>ar()
}
