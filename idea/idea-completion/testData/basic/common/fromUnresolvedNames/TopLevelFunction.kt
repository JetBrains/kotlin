// RUN_HIGHLIGHTING_BEFORE

fun foo(p: Int, x: UnresolvedClassName) {
    unresolvedInFoo1()
    if (p > 0) {
        unresolvedInFoo2()
    }
}

fun <caret>

fun bar() {
    unresolvedInBar()
    val v = unresolvedValue
    listOf(1).filter(::unresolvedFunctionRef)
}

class C {
    fun f() {
        unresolvedInClass()
    }
}


// EXIST: unresolvedInFoo1
// EXIST: unresolvedInFoo2
// EXIST: unresolvedInBar
// EXIST: unresolvedInClass
// ABSENT: UnresolvedClassName
// ABSENT: unresolvedValue
// EXIST: unresolvedFunctionRef
