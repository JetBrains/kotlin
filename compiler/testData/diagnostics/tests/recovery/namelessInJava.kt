// FILE: p/Nameless.java

package p;

public class Nameless {
    void () {}
    int ;
}

// FILE: k.kt

import p.*

class K : <!INVISIBLE_MEMBER!>Nameless<!>() {
    fun<!SYNTAX!><!> () {}
    val<!SYNTAX!><!> : Int = 1
}