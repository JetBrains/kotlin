// !CHECK_TYPE

// FILE: p/J.java

package p;

public class J {
    public J j() {return null;}

    public <T> T foo() {return null;}
    public <T extends J> T foo1() {return null;}
}

// FILE: k.kt

import p.*

fun test(j: J) {
    checkSubtype<J>(j.j())
    j.j().j()
    j.j()!!.j()

    val ann = j.foo<String>()
    ann!!.length
    ann.length

    val a = j.foo<J>()
    a!!.j()
    a.j()
}