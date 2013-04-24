open class Base {
    open fun sayHello(): String {
        return "O"
    }
}

trait Trait: Base {
    override fun sayHello(): String {
        return "K"
    }
}

class Derived(): Base(), Trait {
    override fun sayHello(): String {
        return super<Base>.sayHello() + super<Trait>.sayHello()
    }
}

fun box(): String {
    return Derived().sayHello()
}