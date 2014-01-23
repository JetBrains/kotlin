package test

open class A

class MyClass(
        a: A = run {
            class B: A() {

            }

            B()
        }
)

//package test
//internal open class A defined in test
//public constructor A() defined in test.A
//internal final class MyClass defined in test
//public constructor MyClass(a: test.A = ...) defined in test.MyClass
//value-parameter val a: test.A = ... defined in test.MyClass.<init>
//internal final class B : test.A defined in test.MyClass.<init>.<anonymous>
//public constructor B() defined in test.MyClass.<init>.<anonymous>.B