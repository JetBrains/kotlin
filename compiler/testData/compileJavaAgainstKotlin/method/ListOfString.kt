// FILE: ListOfString.java
package test;

import java.util.List;
import java.util.ArrayList;

class ListString {
    public static void gg() {
        List<String> list = new ArrayList<String>();
        ListOfStringKt.ff(list);
    }
}

// FILE: ListOfString.kt
package test

fun ff(p: List<String>) = 1
