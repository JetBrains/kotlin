class Pair(val first: Int, val second: Int)

fun foo(p: Pair) {
    if (p.<caret>first < p.second)
}

// ELEMENT: hashCode
// CHAR: '\t'
