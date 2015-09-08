package test

open class A

class MyClass() {
    init {
        class B: A() {

        }
    }
}

//package test
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass() defined in test.MyClass
//local final class B : test.A defined in test.MyClass.<init>
//public constructor B() defined in test.MyClass.<init>.B