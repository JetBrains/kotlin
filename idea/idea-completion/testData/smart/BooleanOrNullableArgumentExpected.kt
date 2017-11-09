fun foo(s: String?, flag: Boolean, x: Int){}
fun foo(xx: Boolean){}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "null", attributes: "bold" }
// EXIST: { itemText: "true", attributes: "bold" }
// EXIST: { itemText: "false", attributes: "bold" }
// EXIST: { lookupString: "s = null", itemText: "s = null", attributes: "" }
// EXIST: { lookupString: "xx = true", itemText: "xx = true", attributes: "" }
// EXIST: { lookupString: "xx = false", itemText: "xx = false", attributes: "" }
