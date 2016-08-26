class A

open class X(x: List<A>, f: (<selection>A</selection>) -> Int) {
    val a: A? = x.firstOrNull()
}

class Y : X(listOf(A()), { 1 })

fun test() {
    X(listOf(A())) { 1 }
}