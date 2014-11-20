fun foo(p: (String, Char) -> Unit){}

fun test() {
    fo<caret>
}

// EXIST: { lookupString:"foo", itemText: "foo", tailText: "(p: (String, Char) -> Unit)", typeText:"Unit" }
// EXIST: { lookupString:"foo", itemText: "foo", tailText: " { (String, Char) -> ... }", typeText:"Unit" }
