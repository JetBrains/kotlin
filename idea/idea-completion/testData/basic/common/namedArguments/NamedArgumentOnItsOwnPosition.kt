fun foo(p1: Int, p2: Int, p3: Long) {}

fun usage(param: Long) {
    foo(p1 = 10, <caret>)
}

// LANGUAGE_VERSION: 1.4
// EXIST: param
// EXIST: { itemText: "p2 =" }
// EXIST: { itemText: "p3 =" }
// ABSENT: { itemText: "p1 =" }
