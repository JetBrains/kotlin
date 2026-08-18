// FILE: ListOfInt.java
package test;

import java.util.List;
import java.util.ArrayList;

class ListOfInt {

    public static void hhh() {
        List<Integer> list = new ArrayList<Integer>();
        List<Integer> r = ListOfIntKt.ggg(list);
    }

}

// FILE: ListOfInt.kt
package test

fun ggg(list: List<Int>) = list
