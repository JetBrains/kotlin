trait Foo
trait Bar

trait A {
    fun <T> foo()
    where T : Foo, T : Bar
    = Unit.VALUE
}

class B : A {
    override fun <T> foo()
    where T : Foo, T : Bar
    = Unit.VALUE

}