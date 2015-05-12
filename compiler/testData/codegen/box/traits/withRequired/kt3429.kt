open class Base {
    open fun sayHello(): String{
        return "fail"
    }
}

interface Trait: Base {
    override fun sayHello(): String {
        return "OK"
    }
}

class Derived(): Base(), Trait

fun box() : String {
    return Derived().sayHello()
}