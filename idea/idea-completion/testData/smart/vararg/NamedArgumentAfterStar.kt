fun foo(vararg args: Any){ }

fun bar(s: String, arr: Array<String>){
    foo(args = *<caret>)
}

// ABSENT: s
// EXIST: { lookupString: "arr", itemText: "arr" }
// ABSENT: { lookupString: "arr", itemText: "*arr" }
