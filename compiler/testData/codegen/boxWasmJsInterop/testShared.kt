// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// NODISABLE_WASM_EXCEPTION_HANDLING

// FILE: shared.kt

abstract class MyClassBase {
    abstract fun foo(): Int
}

class MyClass(val a: Int, val b: Int) : Function2<Int, Int, Int>, MyClassBase() {
    override fun invoke(p1: Int, p2: Int): Int = p1 - a + p2 * b

    override fun foo() = 5
}

var myObj = MyClass(1, 2)

fun box(): String {
    val myObjAsBase: MyClassBase = myObj
    val myObjAsFunc: Function2<Int, Int, Int> = myObj
    return if (myObj.a == 1 && myObjAsFunc.invoke(3, 4) == 10 && myObjAsBase.foo() == 5) "OK" else "Fail"
}