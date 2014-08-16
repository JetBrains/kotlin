// FILE: p/J.java

package p;

public class J {
    public void list(java.util.List<String> s) {}
}

// FILE: k.kt

import p.*

fun test(ls: List<String>, mls: MutableList<String>, lsn: List<String?>, mlsn: MutableList<String?>?) {
    J().list(ls)
    J().list(mls)
    J().list(lsn)
    J().list(mlsn)
}