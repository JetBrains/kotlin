annotation class MyAnnotation

open class Base {
    @MyAnnotation
    open fun annotatedFun() {}
}

class Derived : Base()
