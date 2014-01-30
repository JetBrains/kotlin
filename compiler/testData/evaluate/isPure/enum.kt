package test

enum class MyEnum { A }

// val prop1: false
val prop1 = MyEnum.A

// val prop2: false
val prop2 = javaClass<MyEnum>()
