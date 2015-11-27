fun foo(s: List<String>){}

fun foo(i: Map<String, Int>){}

fun bar(o: Any) {
    foo(o as <caret>)
}

// EXIST: { lookupString:"List", itemText:"List<String>", tailText: " (kotlin.collections)" }
// EXIST: { lookupString:"Map", itemText:"Map<String, Int>", tailText: " (kotlin.collections)" }
// NOTHING_ELSE
