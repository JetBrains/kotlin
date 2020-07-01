fun <T : Any> T.anyFun() {}
val <T : Any> T.anyVal: Int get() = 10

open class A
fun <T : A> T.aFun() {}
val <T : A> T.aVal: Int get() = 10

open class B

fun <T : B> T.bFun() {}
val <T : B> T.bVal: Int get() = 10

fun test(a: A) {
    a.aFun()
    a.<caret>
}

// EXIST: anyFun
// EXIST: anyVal
// EXIST: aVal
// EXIST: aFun
// ABSENT: bVal
// ABSENT: bFun