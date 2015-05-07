fun foo(param1: String, param2: Int) { }

fun bar(pInt: Int, pString: String) {
    foo(param2 = <caret>)
}

// EXIST: pInt
// ABSENT: pString
