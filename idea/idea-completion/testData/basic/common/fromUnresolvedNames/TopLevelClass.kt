// RUN_HIGHLIGHTING_BEFORE

fun foo(p: UnresolvedClass1) {
    val foo = UnresolvedClass2()
    val bar = unresolvedValue
}

class <caret>

// EXIST: TopLevelClass
// EXIST: UnresolvedClass1
// EXIST: UnresolvedClass2
// ABSENT: unresolvedValue
