package test

interface BaseInterface {
    class NestedFromInterface
}

open class Base(any: Any): BaseInterface {
    class NestedFromClass
    inner class InnerFromClass
}

class Child : Base(<expr>Base.NestedFromClass()</expr>)