// FILE: A.kt

package a

sealed class Empty

sealed class OnlyNested {
    class Nested : OnlyNested()
}

sealed class NestedAndTopLevel {
    class Nested : NestedAndTopLevel()
}
class TopLevel : NestedAndTopLevel()

// FILE: B.kt

import a.*

// This test checks that we correctly load subclasses of a compiled sealed class from binaries.
// It's not a diagnostic test because there are no diagnostic tests where resolution is performed against compiled Kotlin binaries

fun empty(e: Empty): String = when (e) {
    else -> "1"
}

fun onlyNested(on: OnlyNested): String = when (on) {
    is OnlyNested.Nested -> "2"
}

fun nestedAndTopLevel(natl: NestedAndTopLevel): String = when (natl) {
    is NestedAndTopLevel.Nested -> "3"
    is TopLevel -> "4"
}

fun box(): String {
    return "OK"
}
