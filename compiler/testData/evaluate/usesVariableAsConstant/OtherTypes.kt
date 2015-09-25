package test

enum class MyEnum { A, B }

fun foo(): Boolean = true

val x = 1

// val prop1: false
val prop1 = MyEnum.A

// val prop2: null
val prop2 = foo()

// val prop3: true
val prop3 = "$x"

// val prop4: false
val prop4 = intArrayOf(1, 2, 3)

// val prop5: true
val prop5 = intArrayOf(1, 2, x, x)