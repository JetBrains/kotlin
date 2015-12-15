// "Rename reference" "true"
// ERROR: Unresolved reference: x
// ERROR: Unresolved reference: x
// ERROR: Unresolved reference: x
// ERROR: Unresolved reference: x
// ERROR: Unresolved reference: x
// ERROR: Unresolved reference: x
class A {
    fun f() = 1
    fun g() = ""
}

class B

fun bar(i: Int) {

}

fun baz(i: Int) {

}

fun foo(a: A, b: B) {
    val aa = A()
    bar(a.x)
    baz(a.x)
    bar(a.<caret>x())
    baz(a.x())
    bar(a.x(1))
    baz(a.x(1))
    bar(aa.x())
    bar(b.x())
}