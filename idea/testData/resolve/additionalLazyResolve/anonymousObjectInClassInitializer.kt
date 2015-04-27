package test

open class A

class MyClass() {
    init {
        val a = object: A() {

        }
    }
}

//package test
//internal open class A defined in test
//public constructor A() defined in test.A
//internal final class MyClass defined in test
//public constructor MyClass() defined in test.MyClass
//val a: test.MyClass.<init>.<no name provided> defined in test.MyClass.<init>
//local final class <no name provided> : test.A defined in test.MyClass.<init>
//internal constructor <no name provided>() defined in test.MyClass.<init>.<no name provided>