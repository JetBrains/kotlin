package ppp

import java.io.*

class X {
    public class PublicNested
    private class PrivateNested

    fun f1(nested: PublicNested) { }
    fun f2(nested: PrivateNested) { }
    fun f3(nestedList: List<PublicNested>) { }
    fun f4(nestedList: List<PrivateNested>) { }
}

fun foo(neste<caret>)

// EXIST: { lookupString: "nested", itemText: "nested: X.PublicNested", tailText: " (ppp)" }
// EXIST: { lookupString: "nestedList", itemText: "nestedList: List<X.PublicNested>", tailText: " (kotlin)" }
// NOTHING_ELSE
