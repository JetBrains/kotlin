
class Bar

class Foo {
    fun bar() = this
    fun Bar.buz() = this
    fun Bar.foo() = this@Foo
    fun Bar.foobar() = this@foobar
}