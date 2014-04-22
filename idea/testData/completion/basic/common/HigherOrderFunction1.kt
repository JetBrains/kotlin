fun foo(p: (String, Char) -> Unit){}

fun test() {
    fo<caret>
}

// EXIST: { lookupString:"foo", itemText: "foo(p: (String, Char) -> Unit)", typeText:"Unit" }
// EXIST: { lookupString:"foo", itemText: "foo { (String, Char) -> ... }", typeText:"Unit" }
