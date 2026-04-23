// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// IGNORE_BACKEND: NATIVE, WASM_JS, WASM_WASI, JS_IR, JS_IR_ES6
// ^ The support will be added later

val list = mutableListOf<Any>()

sealed value class Sealed(a: Int) {
    init {
        list.add(a - 100)
    }
}

abstract value class A(a: Int): Sealed(a - 100) {
    init {
        list.add(a)
    }
}

value class B(val x: Int): A(-x) {
    init {
        list.add(x)
    }
}

fun id(b: B) = b

fun B.cast(): A = this
fun A.cast(): B = this as B
fun B.asNullable(): B? = this

fun box(): String {
    val b = B(42)
    val b1 = id(b)
    val a = b1.cast()
    require(a as Any is B)
    require(a as Any is A)
    val b2 = a.cast()
    val b3 = id(b2)
    val b4 = b3.asNullable()
    val b5 = b4!!

    require(list == listOf(-242, -42, 42)) { list.toString() }

    return "OK"
}
