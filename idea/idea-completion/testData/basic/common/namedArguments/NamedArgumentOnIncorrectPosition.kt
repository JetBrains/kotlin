fun foo(p1: Int, p2: Int, p3: Long) {}

fun usage(param: Long) {
    foo(p2 = 10, <caret>)
}

// LANGUAGE_VERSION: 1.4
// EXIST: { itemText: "p1 =" }
// EXIST: { itemText: "p3 =" }
// NOTHING_ELSE
