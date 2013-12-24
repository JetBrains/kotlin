package test

open class A

class MyClass() {
    {
        class B: A() {

        }
    }
}

//package test
//internal open class A defined in test
//public constructor A() defined in test.A
//internal final class MyClass defined in test
//public constructor MyClass() defined in test.MyClass
//internal final class B : test.A defined in test.MyClass.<init>
//public constructor B() defined in test.MyClass.<init>.B