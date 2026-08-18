// FILE: ListOfT.java
package test;

import java.util.List;
import java.util.ArrayList;

class ListOfT {

    public static void check() {
        List<String> list = new ArrayList<String>();
        List<String> r = ListOfTKt.listOfT(list);
    }

}

// FILE: ListOfT.kt
package test

fun <P> listOfT(list: List<P>) = list
