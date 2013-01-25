open class MyClass(param : String) {
    var propterty = param
}

trait MyTrait : MyClass
{
    fun foo()  = propterty
}

open class B(param : String) : MyTrait, MyClass(param)
{
    override fun foo() = super<MyTrait>.foo()
}

fun box()= B("OK").foo()
