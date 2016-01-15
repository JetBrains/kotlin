// RUN_HIGHLIGHTING_BEFORE

fun foo(p: UnresolvedClass1) {
    val foo = UnresolvedClass2()
    val bar = unresolvedValue
}

interface <caret>

// EXIST: TopLevelInterface
// EXIST: UnresolvedClass1
// ABSENT: UnresolvedClass2
// ABSENT: unresolvedValue
