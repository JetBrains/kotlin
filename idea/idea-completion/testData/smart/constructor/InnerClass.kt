trait T

class C {
    inner class Inner1(s: String) : T
    inner class Inner2 : T
    inner class Inner3
    class Nested: T
}

fun foo(c: C): T {
    return c.<caret>
}

// EXIST: { lookupString: "Inner1", itemText: "Inner1", tailText: "(s: String)", typeText: "C.Inner1" }
// EXIST: { lookupString: "Inner2", itemText: "Inner2", tailText: "()", typeText: "C.Inner2" }
// ABSENT: Inner3
// ABSENT: Nested
// NUMBER: 2
