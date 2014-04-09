package test

enum class MyEnum { A;B }

fun foo(): Boolean = true

val x = 1

// val prop1: null
val prop1 = MyEnum.A

// val prop2: null
val prop2 = foo()

// val prop3: true
val prop3 = "$x"