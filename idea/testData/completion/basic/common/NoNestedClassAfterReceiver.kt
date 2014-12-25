class C {
    class Nested
    inner class Inner1
    inner class Inner2(s: String)
}

fun foo(c: C) {
    c.<caret>
}

// ABSENT: Nested
// EXIST: { lookupString: "Inner1", itemText: "Inner1", tailText: "()", typeText: "C.Inner1" }
// EXIST: { lookupString: "Inner2", itemText: "Inner2", tailText: "(s: String)", typeText: "C.Inner2" }
