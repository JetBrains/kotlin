fun foo(vararg args: Any){ }
fun foo(s: String){ }

fun bar(s: String, arr: Array<String>){
    foo(*<caret>)
}

// ABSENT: s
// EXIST: { lookupString: "arr", itemText: "arr" }
// ABSENT: { lookupString: "arr", itemText: "*arr" }
