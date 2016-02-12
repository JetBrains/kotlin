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
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass(a: test.A = ...) defined in test.MyClass
//value-parameter a: test.A = ... defined in test.MyClass.<init>
//local final class B : test.A defined in test.MyClass.<init>.<anonymous>
//public constructor B() defined in test.MyClass.<init>.<anonymous>.B