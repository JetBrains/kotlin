package p.q

<selection>fun foo(myC: p.q.MyClass, def: p.q.MyClass.Companion, nes: p.q.MyClass.Companion.Nested) {
    p.q.MyClass.Companion.foo()
    p.q.MyClass.Companion.coProp
    p.q.MyClass.Companion
    p.q.MyClass
    p.q.MyClass.coProp
    p.q.MyClass.foo()
    p.q.MyClass.bar(p.q.MyClass.Companion)
    p.q.MyClass.bar(p.q.MyClass)
    p.q.MyClass.Companion.Nested.Companion
    p.q.MyClass.Companion.Nested.Companion.c
    MyClass.Companion
}</selection>

class MyClass {
    companion object {
        val coProp = 1

        class Nested {
            companion object {
                val c: Int = 1
            }
        }

        fun foo() {

        }

        fun bar(p: MyClass.Companion) {
        }
    }
}