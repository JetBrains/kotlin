fun foo(list: List<String>, intList: MutableList<Int>, stringList: MutableList<String>, p: Any) {
    list.filterTo(<caret>)
}

// EXIST: arrayListOf
// EXIST: stringList
// ABSENT: intList
// ABSENT: p
