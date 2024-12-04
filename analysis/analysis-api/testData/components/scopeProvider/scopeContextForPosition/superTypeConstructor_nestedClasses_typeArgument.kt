package test

interface BaseInterface {
    class NestedFromInterface
}

open class Base<T> : BaseInterface {
    class NestedFromClass
    inner class InnerFromClass
}

class Child : Base<<expr>Base.NestedFromClass</expr>>()