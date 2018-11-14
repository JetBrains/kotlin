fun foo(p: Int, flag: Boolean){}

fun bar() {
    foo(1, <caret>)
}

// WITH_ORDER
// EXIST: { itemText: "true", attributes: "bold" }
// EXIST: { itemText: "false", attributes: "bold" }
// EXIST: { lookupString: "flag = true", itemText: "flag = true", attributes: "" }
// EXIST: { lookupString: "flag = false", itemText: "flag = false", attributes: "" }
// EXIST: { lookupString: "maxOf", tailText:"(a: Boolean, b: Boolean) (kotlin.comparisons)", typeText:"Boolean"}
// EXIST: { lookupString: "maxOf", tailText:"(a: Boolean, b: Boolean, c: Boolean) (kotlin.comparisons)", typeText:"Boolean"}
// EXIST: { lookupString: "minOf", tailText:"(a: Boolean, b: Boolean) (kotlin.comparisons)","typeText":"Boolean"}
// EXIST: { lookupString: "minOf", tailText:"(a: Boolean, b: Boolean, c: Boolean) (kotlin.comparisons)", typeText:"Boolean"}
// EXIST: {"lookupString":"equals","tailText":"(other: Any?) (kotlin)","typeText":"Boolean"}
// NOTHING_ELSE
