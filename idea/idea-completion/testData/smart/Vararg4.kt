fun foo(vararg args: Any){ }

fun bar(s: String, arr: Array<String>){
    foo(<caret>)
}

// EXIST: s
// EXIST: { lookupString: "arr", itemText: "arr" }
// EXIST: { lookupString: "arr", itemText: "*arr" }
