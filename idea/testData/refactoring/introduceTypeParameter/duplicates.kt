class A

fun foo(x: List<A>, f: (<selection>A</selection>) -> Int) {
    val a: A? = x.firstOrNull()
}

fun test() {
    foo(listOf(A())) { 1 }
}