package test


open class Base<T>()

class MyClass<MyType> {

    interface MyType

    <expr>
    context(MyClass.MyType)
    class Nested<Other : MyClass.MyType> : Base<MyClass.MyType>()
    </expr>
}
