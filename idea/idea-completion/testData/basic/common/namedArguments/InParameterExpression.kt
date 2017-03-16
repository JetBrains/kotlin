val paramTest = 12

fun small(paramFirst: Int, paramSecond: Int) {
}

fun test() = small(paramFirst = param<caret>)

// EXIST: paramTest
// ABSENT: { itemText: "paramFirst =" }
// ABSENT: { itemText: "paramSecond =" }
// NOTHING_ELSE
