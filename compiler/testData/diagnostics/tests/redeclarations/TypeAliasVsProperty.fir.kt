// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
typealias Test = String

val Test = 42

class Outer {
    typealias Test = String

    val Test = 42
}

typealias Test2 = String

// FILE: file2.kt
val Test2 = 42
