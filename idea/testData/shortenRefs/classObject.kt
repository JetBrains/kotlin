package p.q

<selection>fun foo(myC: p.q.MyClass, def: p.q.MyClass.Default, nes: p.q.MyClass.Default.Nested) {
    p.q.MyClass.Default.foo()
    p.q.MyClass.Default.coProp
    p.q.MyClass.Default
    p.q.MyClass
    p.q.MyClass.coProp
    p.q.MyClass.foo()
    p.q.MyClass.bar(p.q.MyClass.Default)
    p.q.MyClass.bar(p.q.MyClass)
    p.q.MyClass.Default.Nested.Default
    p.q.MyClass.Default.Nested.Default.c
    MyClass.Default
}</selection>

class MyClass {
    default object {
        val coProp = 1

        class Nested {
            default object {
                val c: Int = 1
            }
        }

        fun foo() {

        }

        fun bar(p: MyClass.Default) {
        }
    }
}