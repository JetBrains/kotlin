// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
class SomeClass

typealias SomeClass = Any
typealias SomeClass = Any
typealias SomeClass = Any

class Outer {
    class Nested

    typealias Nested = Any
    typealias Nested = Any
    typealias Nested = Any
}

// FILE: file2.kt
typealias SomeClass = Any