// ISSUE: KT-67593

interface Foo {
    fun bar(x: Int, y: String? = null): String
}

open class FooFoo(val f: Foo) : Foo by f

class Final(f: Foo) : FooFoo(f) {
    override fun bar(x: Int, y: String?): String {
        return super.bar(x)
    }
}
