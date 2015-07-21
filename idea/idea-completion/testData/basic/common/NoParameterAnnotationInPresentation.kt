fun foo(inlineOptions p: () -> Unit){}

fun bar() {
    <caret>
}

// EXIST: { lookupString:"foo", itemText: "foo", tailText: " {...} (p: () -> Unit) (<root>)", typeText:"Unit" }
