fun foo(list: List<String>, intList: MutableList<Int>, stringList: MutableList<String>, p: Any): Collection<Int> {
    return list.mapTo(<caret>)
}

// EXIST: intList
// EXIST: arrayListOf
// EXIST: stringList
// ABSENT: p
