// ORIGINAL: /compiler/testData/diagnostics/tests/inference/regressions/kt34029.fir.kt
// WITH_STDLIB
open class MyClass<T> {
    object MyObject : MyClass<Boolean>() { }
}

val foo1 = MyClass.MyObject // it's ok
val foo2 = MyClass<Boolean>.MyObject // here's stofl


fun box() = "OK"
