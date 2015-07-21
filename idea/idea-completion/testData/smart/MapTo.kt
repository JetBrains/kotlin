fun foo(list: List<String>, intList: MutableList<Int>, stringList: MutableList<String>): Collection<Int> {
    return list.mapTo(<caret>)
}

// EXIST: intList
// EXIST: arrayListOf
// EXIST: stringList
