data class Data(val val1: Int, val val2: String)

fun foo(d: Data) {
    d.<caret>
}

// EXIST: val1
// EXIST: val2
// ABSENT: component1
// ABSENT: component2