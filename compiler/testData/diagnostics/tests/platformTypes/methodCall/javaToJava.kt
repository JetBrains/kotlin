// FILE: p/J.java

package p;

import java.util.ArrayList;

public class J {
    public ArrayList<String> list() { return null; }
    public void takeList(ArrayList<String> list) { }
}

// FILE: k.kt

import p.*

fun test(j: J) {
    j.takeList(j.list())
    val l = j.list()
    j.takeList(l)
}