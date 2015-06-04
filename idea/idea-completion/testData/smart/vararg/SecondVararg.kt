fun foo(vararg v1: Int, vararg v2: Char, s: String) { }

fun bar(c: Char, pInt: Int) {
    foo(*intArrayOf(), <caret>)
}

// EXIST: c
// ABSENT: pInt
// EXIST: { lookupString: "charArrayOf", itemText: "*charArrayOf" }
