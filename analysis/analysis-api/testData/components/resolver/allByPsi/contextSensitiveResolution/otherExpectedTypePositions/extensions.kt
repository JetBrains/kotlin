// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

class MyClass {
    companion object {
        val MyClass.Companion.bar: MyClass get() = MyClass()
    }
}

fun foo(a: MyClass) {}

fun main() {
    foo(MyClass.bar)
    foo(bar)
}

class MyClass2 {
    companion object
}

val MyClass2.Companion.bar: MyClass2 get() = MyClass2()

fun foo2(a: MyClass2) {}

fun main2() {
    foo2(MyClass2.bar)
    foo2(bar)
}
