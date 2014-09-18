class C {
    fun foo(){}
    fun bar(p: Int) {}
    fun zoo(p1: Int, p2: Int){}
    val prop: Int = 1
}

fun C.xxx() {}
fun C.yyy(p: Int) {}
fun C.zzz(p1: Int, p2: Int) {}
val C.extensionProp: Int get() = 1

fun <A, B> A.and(that: B): Pair<A, B> = Pair(this, that)

fun String.ttt(p: Int) {}

fun f() {
    C() <caret>
}

// ABSENT: "foo"
// EXIST: "bar"
// ABSENT: "zoo"
// ABSENT: "prop"

// ABSENT: "xxx"
// EXIST: "yyy"
// ABSENT: "zzz"
// ABSENT: "extensionProp"

// EXIST: "and"

// ABSENT: "ttt"
