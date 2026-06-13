open class Base {
    open fun <T> genericFun(x: T): T = x
}

class Derived : Base()
