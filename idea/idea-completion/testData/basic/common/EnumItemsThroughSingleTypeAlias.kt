enum class A {
    ONE;
    class B // Not allowed to resolve through typealiases
}

typealias AA = A

fun usage() {
    AA.<caret>
}

// EXIST: ONE, values, valueOf
// ABSENT: B