fun foo(p: (String, Char) -> Unit): String {}

fun v: String = fo<caret>

// EXIST: { lookupString:"foo", itemText: "foo", tailText: "(p: (String, Char) -> Unit) (<root>)", typeText:"String" }
// EXIST: { lookupString:"foo", itemText: "foo", tailText: " { String, Char -> ... } (p: (String, Char) -> Unit) (<root>)", typeText:"String" }
