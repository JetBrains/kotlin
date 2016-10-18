fun foo(p: Int, flag: Boolean){}

fun bar() {
    foo(1, <caret>)
}

// WITH_ORDER
// EXIST: { itemText: "true", attributes: "bold" }
// EXIST: { itemText: "false", attributes: "bold" }
// EXIST: { lookupString: "flag = true", itemText: "flag = true", attributes: "" }
// EXIST: { lookupString: "flag = false", itemText: "flag = false", attributes: "" }
// NOTHING_ELSE
