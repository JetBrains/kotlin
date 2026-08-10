// FILE: Vararg.java
package test;

import java.util.List;
import java.util.ArrayList;

class Vararg {
    {
        List<String> list = new ArrayList<String>();
        List<String> r = VarargKt.gg(list, 3, 4, 5, 6);
    }
}

// FILE: Vararg.kt
package test

fun gg(list: List<String>, vararg ints: Int) = list
