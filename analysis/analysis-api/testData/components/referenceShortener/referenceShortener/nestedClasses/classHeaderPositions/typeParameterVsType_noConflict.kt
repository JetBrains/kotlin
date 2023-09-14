package test

interface MyType

open class Base<T>()

class MyClass<MyType> {
    <expr>
    context(test.MyType)
    class Nested<Other : test.MyType> : Base<test.MyType>()
    </expr>
}
