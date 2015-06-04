fun foo(vararg v1: Int, vararg v2: Char, s: String) { }

fun bar(c: Char, pInt: Int) {
    foo(*intArrayOf(), <caret>)
}

// ABSENT: c
// ABSENT: { lookupString: "charArrayOf", itemText: "*charArrayOf" }
// EXIST: pInt
// EXIST: { lookupString: "intArrayOf", itemText: "*intArrayOf" }
