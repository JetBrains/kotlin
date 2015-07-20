fun foo(list: List<String>, intList: MutableList<Int>, stringList: MutableList<String>): Collection<Int> {
    list.mapTo(<caret>)
}

// EXIST: intList
// EXIST: stringList
// EXIST: arrayListOf
