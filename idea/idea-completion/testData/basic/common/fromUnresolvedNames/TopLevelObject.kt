// RUN_HIGHLIGHTING_BEFORE

fun foo(p: UnresolvedClass1) {
    val foo = UnresolvedClass2()
    val bar = unresolvedValue
}

object <caret>

// EXIST: TopLevelObject
// EXIST: UnresolvedClass1
// ABSENT: UnresolvedClass2
// EXIST: unresolvedValue
