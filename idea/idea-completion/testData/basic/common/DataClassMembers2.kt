data class Data(val val1: Int, val val2: String)

fun foo(d: Data) {
    d.comp<caret>
}

// EXIST: component1
// EXIST: component2