package p.q

<selection>fun foo(): Int {
    p.q.MyClass.foo()
    return p.q.MyClass.coProp + 10
}</selection>

class MyClass {
    class object {
        val coProp = 1

        fun foo() {

        }
    }
}