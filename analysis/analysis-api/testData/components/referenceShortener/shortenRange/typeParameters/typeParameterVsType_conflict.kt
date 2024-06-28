package test

interface MyType

open class Base<T>()

<expr>
context(test.MyType)
class MyClass<MyType, Other : test.MyType> : Base<test.MyType>()
</expr>