// IGNORE_BACKEND_FIR: JVM_IR
var result = "Fail"

interface MyTrait
{
    var property : String
    fun foo()  {
        result = property
    }
}

open class B(param : String) : MyTrait
{
    override var property : String = param
    override fun foo() {
        super.foo()
    }
}

fun box(): String {
    val b = B("OK")
    b.foo()
    return result
}
