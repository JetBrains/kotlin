package test

open class A

class MyClass(
        a: A = object: A() {

        }
)

//package test
//internal open class A defined in test
//public constructor A() defined in test.A
//internal final class MyClass defined in test
//public constructor MyClass(a: test.A = ...) defined in test.MyClass
//value-parameter val a: test.A = ... defined in test.MyClass.<init>
//internal final class <no name provided> : test.A defined in test.MyClass.<init>
//private constructor <no name provided>() defined in test.MyClass.<init>.<no name provided>