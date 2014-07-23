fun foo(s: List<String>){}

fun foo(i: Map<String, Int>){}

fun bar(o: Any) {
    foo(o as <caret>)
}

// NUMBER: 2
// EXIST: { lookupString:"List", itemText:"List<String>" }
// EXIST: { lookupString:"Map", itemText:"Map<String, Int>" }
