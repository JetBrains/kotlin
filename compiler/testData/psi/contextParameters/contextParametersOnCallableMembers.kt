// LANGUAGE: +ContextParameters

annotation class MyAnnotation
class A {
    val x = 1
}
class B

class ContextParametersOnCallableMembers {
    context(a: A, _: B)
    @MyAnnotation
    fun Int.function(): Int = a.x

    context(a: A, _: B)
    @MyAnnotation
    val Int.property: Int get() = a.x

    context(a: A, _: B)
    @MyAnnotation
    var Int.propertyWithSetter: Int
        get() = a.x
        set(v) { }
}
