fun foo(param1: String, param2: Int, param3: Char) { }

fun bar(pInt: Int, pString: String) {
    foo(param2 = , <caret>)
}

// EXIST: { lookupString: "param1", itemText: "param1 =" }
// EXIST: { lookupString: "param3", itemText: "param3 =" }
// ABSENT: param2
// NOTHING_ELSE
