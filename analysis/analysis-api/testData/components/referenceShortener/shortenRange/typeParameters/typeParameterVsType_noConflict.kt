package test

interface MyType

open class Base<T>()

class MyClass<MyType> {
    // TODO this test has an error, MyType should not be resolved here. See KT-61959
    <expr>
    context(test.MyType)
    class Nested<Other : test.MyType> : Base<test.MyType>()
    </expr>
}
