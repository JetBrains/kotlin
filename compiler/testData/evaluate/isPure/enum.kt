package test

enum class MyEnum { A }

// val prop1: true
val prop1 = MyEnum.A

// val prop2: null
val prop2 = javaClass<MyEnum>()
