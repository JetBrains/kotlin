package p.q

val a = 1
fun foo() = 1

// SIBLING:
class MyClass {
    fun test() {
        <selection>p.q.foo()
        p.q.a</selection>
    }
}