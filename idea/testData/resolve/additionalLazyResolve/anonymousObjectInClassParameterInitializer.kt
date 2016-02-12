package test

open class A

class MyClass(
        a: A = object: A() {

        }
)

//package test
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass(a: test.A = ...) defined in test.MyClass
//value-parameter a: test.A = ... defined in test.MyClass.<init>
//local final class <no name provided> : test.A defined in test.MyClass.<init>
//public constructor <no name provided>() defined in test.MyClass.<init>.<no name provided>