fun foo(first: Int, second: Int, third: String) {
}

fun test(p: Int) = foo(12, <caret>, third = "")

// EXIST: p
// ABSENT: "first ="
// EXIST: "third ="
// EXIST: "second ="
