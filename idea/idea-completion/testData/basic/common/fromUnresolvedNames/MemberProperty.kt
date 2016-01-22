// RUN_HIGHLIGHTING_BEFORE

class C {
    fun foo(p: Int) {
        print(unresolvedInFoo)
    }

    val <caret>

    fun bar(s: String, x: UnresolvedType) {
        print(unresolvedInBar)
        print(s.unresolvedWithReceiver)
    }
}

fun f() {
    print(unresolvedOutside)
}

// EXIST: unresolvedInFoo
// EXIST: unresolvedInBar
// ABSENT: unresolvedWithReceiver
// ABSENT: unresolvedOutside
// ABSENT: UnresolvedType
