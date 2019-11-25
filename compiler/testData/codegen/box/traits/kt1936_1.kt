// IGNORE_BACKEND_FIR: JVM_IR
interface MyTrait
{
    var property : String
    fun foo() = property
}

open class B(param : String) : MyTrait
{
    override var property : String = param
    override fun foo() = super.foo()
}

fun box()= B("OK").foo()
