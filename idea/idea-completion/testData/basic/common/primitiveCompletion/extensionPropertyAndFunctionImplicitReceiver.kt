fun Any.anyFun() {}
val Any.anyVal: Int get() = 10

class A
fun A.aFun() {}
val A.aVal: Int get() = 10

class B
fun B.bFun() {}
val B.bVal: Int get() = 10

fun test(a: A) {
    a.run {
        <caret>

        Unit
    }
}

// EXIST: anyFun
// EXIST: anyVal
// EXIST: aVal
// EXIST: aFun
// ABSENT: bVal
// ABSENT: bFun